import os
import logging
from datetime import datetime
import amazon.ion.simpleion as ion
import boto3

logger = logging.getLogger()
logger.setLevel(logging.INFO)


def handler(event, context):
    bucket = os.environ['ExportJournalBucket']
    prefix = os.environ['ExportJournalPrefix']
    s3_client = boto3.client('s3')
    s3 = boto3.resource('s3')
    qldb_client = boto3.client('qldb')
    object_response_paginator = s3_client.get_paginator('list_objects')
    operation_parameters = {'Bucket': bucket,
                            'Prefix': prefix,
                            'Delimiter': '/',
                            }
    in_progress_list, completed_list, started_list = [], [], []

    # Create list of on-going and not archived manifests
    for object_response_itr in object_response_paginator.paginate(**operation_parameters):
        try:
            for obj in object_response_itr['Contents']:
                if obj['Key']:
                    file = s3.Object(bucket, obj['Key'])
                    body = file.get()['Body'].read()
                    obj['Body'] = ion.loads(str(body, 'utf-8'))
                    if "completed.manifest" in obj['Key']:
                        completed_list.append(obj)
                    if "started.manifest" in obj['Key']:
                        started_list.append(obj)
        except KeyError as e:
            logger.info("Export Bucket Empty")
    logger.info("Completed objects: {}".format(len(completed_list)))
    logger.info("Started objects: {}".format(len(started_list)))

    ## Determine date of last successful export and check if export is not in progress
    for i in started_list:
        if i['Key'].split(".")[0] not in [x['Key'].split(".")[0] for x in completed_list]:
            in_progress_list.append(i)
    if len(in_progress_list) > 0:
        logger.warning("In Progress objects: {}".format(len(in_progress_list)))
        for export in in_progress_list:
            export_id = export['Body']['exportId']
            logger.info("Checking status of exportid: {}".format(export_id))
            try:
                response = qldb_client.describe_journal_s3_export(
                    Name=os.environ['QLDBName'],
                    ExportId=export_id
                )
                logger.info(response)
            except qldb_client.exceptions.ResourceNotFoundException:
                ## for failed exports move data to "failed" suffix
                logger.error("ExportId {} not Found!".format(export_id))
                logger.error("Moving {} to {}".format(export['Key'], "failed/" + export['Key']))
                s3.Object(bucket, "manifests/failed/" + export['Key']).copy_from(CopySource=bucket + "/" + export['Key'])
                s3.Object(bucket, export['Key']).delete()
        return "Export is currently in progress...."
    if len(started_list) == 0:
        inclusive_start_time = datetime(2019, 1, 1, 0, 0)
    else:
        inclusive_start_time = max(d['Body']['exclusiveEndTime'] for d in started_list)
    logger.info("New InclusiveStartTime: {}".format(inclusive_start_time))

    ## Initialize qldb journal export
    response = qldb_client.export_journal_to_s3(
        Name=os.environ['QLDBName'],
        InclusiveStartTime=inclusive_start_time,
        ExclusiveEndTime=datetime.now(),
        S3ExportConfiguration={
            'Bucket': os.environ['ExportJournalBucket'],
            'Prefix': os.environ['ExportJournalPrefix'],
            'EncryptionConfiguration': {
                'ObjectEncryptionType': 'NO_ENCRYPTION'
            }
        },
        RoleArn=os.environ['ExportJournalRole']
    )
    logger.info(response)

    ## Clenup completed manifests file to "manifests/completed/" directory sorted by date.
    for key in started_list:
        completed_prefix = "manifests/completed/" + key['Body']['exclusiveEndTime'].strftime('%Y/%m/%d/%H/')
        object_name = key['Key']
        new_object_path = completed_prefix + object_name.replace(prefix, "")
        for completed in completed_list:
            if completed['Key'].split(".")[0] in object_name.split(".")[0]:
                completed_key = completed_prefix + completed['Key'].replace(prefix, "")
                logger.info("Moving {} to {}".format(completed['Key'], completed_key))
                s3.Object(bucket, completed_key).copy_from(
                    CopySource=bucket + "/" + completed['Key'])
                s3.Object(bucket, completed['Key']).delete()
        logger.info("Moving {} to {}".format(object_name, new_object_path))
        s3.Object(bucket, new_object_path).copy_from(CopySource=bucket + "/" + object_name)
        s3.Object(bucket, object_name).delete()
