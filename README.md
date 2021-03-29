# Smart Limpid Graph Ledger

### Introduction

The purpose of SLGL (Smart Limpid Graph Ledger) is to provide smart contract functionality on top of AWS QLDB so that they can be trusted by everyone, not just by the AWS account owner. This repository contains source code which can be used to create a full AWS stack with a fully functional solution.

To achieve the smart contract part, SLGL delivers a web service which allows to create nodes and link/unlink them. A node can be linked to another node’s anchor, and every anchor can define conditions which need to be met when linking. By properly defining a set of nodes and its anchors it is possible to model a smart contract.

To achieve the trust part:
1. the project is open source and includes both scripts to build the AWS stack and source code of all the AWS lambdas
2. every significant release will be verified by a trusted organization, which certifies that actual code is working as we describe it here
3. users deploying SLGL on their AWS account are advised to widely provide access to the "audit" IAM role, preferably this access should be easily available for everyone using that SLGL
4. it is advised for an AWS account owner not to use root access and use IAM roles for different functions instead; every root login is reported and can be seen in the audit mode
5. highly certified AWS QLDB is used as a blockchain database, and QLDB journal is published in a publicly available and replicable S3 bucket within an average of 5 minutes


## How to deploy

Latest version of SLGL can be easly deployed on your AWS account. All you need to do is to login into your [AWS console](https://console.aws.amazon.com/) and click the button below:

[![Launch SLGL Stack](https://cdn.rawgit.com/buildkite/cloudformation-launch-stack-button-svg/master/launch-stack.svg)](https://eu-west-1.console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks/create/review?templateURL=https://s3.eu-west-1.amazonaws.com/slgl-artifact-github-bucket/latest/template-export.yml&stackName=slgl)

More information about deploying SLGL can be found in [infrastructure documentation](infrastructure/README.md).


## SLGL in action

Everything needed to access SLGL API can be found in outputs of SLGL stack.

Newly created stack is initialized with single admin user. Username and API key for that admin user can be found in outputs `AdminUsername` and `DefaultAdminApiKey`. 
URL needed to access API can be found in output `ApiUrl`.

Request to SLGL API can be made using any software capable of doing HTTP REST requests.

Example using `curl` command line tool:
```bash
curl <API_URL> -u <USERNAME>:<API_KEY> -H 'Content-Type: application/json' -d '<REQUEST_BODY>' 
``` 

### Create simple node

Creating new node is SLGL is very simple:

```bash
curl <API_URL> -u <USERNAME>:<API_KEY> -H 'Content-Type: application/json' \
-d '{
  "requests" : [ {
    "node" : {
      "foo" : "example",
      "bar": 42
    }
  } ]
}'
```

Executing that request results in following response:
```json
{
  "responses": [
    {
      "node": {
        "@id": "b375b8ca-625f-40d8-9a2d-39cf88b84e07",
        "@state": {},
        "created": "2021-03-19T14:39:30.835365Z",
        "object_sha3": "4a6000be8fcdb84bf0abc9c8a087c692e60ca5b98d74cae7f58fccb493c1afc0534528d374c40470e82951064c519cc0d16a8752ccf87941837680ebc78f60cf"
      }
    }
  ]
}
```

Field `@id` in that response is identifier of newly created node. Field `object_sha3` is hash that can be used to prove what data were send in request.

All public information saved in SLGL (this includes information about node created by above example) is stored in AWS QLDB. This guarantees that data stored in SLGL can't be changed without everyone noticing it.

You can easily verify data stored by SLGL in AWS QLDB, because SLGL automatically exports all data from QLDB to S3. Name of the S3 bucket used for that export can be found in stack output `ExportJournalBucket`. 

### More examples

- [how to create new user](examples/create-new-user.md)
- [real-world smart contract: Princess in love](examples/princess-in-love.md)

## Repository structure

This repository has following structure:

Directory | Description
----------|------------
`/lambda/api` | Lambda for SLGL API requests processing
`/lambda/api_authorizer` | Lambda for SLGL API authorization 
`/lambda/trustlist_cache_refresher` | Lambda for caching EIDAS trusted certificate lists
`/lambda/setup` | Lambda for initial setup and updates that can't be done using CloudFormation template
`/lambda/stream_processor` | Lambda for processing stream of QLDB blocks
`/lambda/qldb_export` | Lambda for periodic exports of QLDB journal to S3 bucket
`/lib/slgl_client` | Java library that simplifies making requests to SLGL API
`/lib/permission` | Java library responsible for processing SLGL permissions
`/lib/template` | Java library responsible for processing SLGL templates
`/infrastructure` | `[TODO write me]`
`/tests` | `[TODO do we need this?]`

## How to build and deploy development environment

### Prerequirements

You must install following software if you want to be able to build SLGL: 

* Java 11
* NodeJS 10
* Python 3.7
* [AWS CLI (version 1)](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv1.html)
* [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
* AWS account

### Building and deploying

Compile SLGL source code:

```bash
sam build
```

Create new AWS S3 bucked (unless you already have it - as this bucket can be reused for multiple deployments):

```bash
aws s3 mb s3://<YOUR_S3_BUCKET_FOR_STORING_PACKAGES>
```

Package and upload compiled code to that AWS S3 bucket: 
```bash
sam package --s3-bucket <YOUR_S3_BUCKET_FOR_STORING_PACKAGES> --output-template-file packaged.yaml
```

Deploy SLGL to AWS:

```bash
sam deploy \
    --stack-name <NAME_OF_SLGL_STACK> \
    --template-file ./packaged.yaml \
    --capabilities CAPABILITY_IAM \
    --parameter-overrides \
    IncludeIntegrationTestsResources=true \
    IncludeExportJournal=false DevUser=<NAME_OF_YOUR_AWS_USER>
```

This command will create new AWS CloudFormation stack. The same command can also be used to update existing stack.
As a result all required AWS resources (like lambdas or QLDB ledger) will be created and initial setup of that resources will be performed.

Last two lines of that command contains configuration parameters.
Setting `IncludeIntegrationTestsResources` to `true` will result in creation of some additions resources that are required to run integration tests.
You should also provide user name (via `DevUser`) or role name (via `DevRole`) that you will use to run integration tests - this will ensure all required permissions will be granted to that user (or role).  

### Running integration tests

To run integration tests you can use following command:

```bash
./gradlew integrationTest -Ddev.slgl.stackName=<NAME_OF_SLGL_STACK>
```

No configuration is required here - you only need to provide name of CloudFormation stack where SLGL was deployed.
All necessary information (like URL of SLGL API) will be automatically extracted from stack outputs.

### Cleanup

To delete deployed SLGL stack and the bucket that you created, use following commands:

```bash
aws cloudformation delete-stack --stack-name <NAME_OF_SLGL_STACK>
aws s3 rb s3://<YOUR_S3_BUCKET_FOR_STORING_PACKAGES>
```
