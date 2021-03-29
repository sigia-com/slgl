package io.slgl.api.config;

import io.slgl.api.ExecutionContext;
import io.slgl.api.ExecutionContextModule;
import io.slgl.api.repository.TransactionManager;
import io.slgl.api.utils.LambdaEnv;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.qldbsession.QldbSessionClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.qldb.QldbDriver;
import software.amazon.qldb.RetryPolicy;

public class AwsModule implements ExecutionContextModule {

    @Override
    public void configure() {
        ExecutionContext.put(QldbDriver.class, qldbDriver());
        ExecutionContext.put(DynamoDbClient.class, amazonDynamoDB());
        ExecutionContext.put(SqsClient.class, amazonSQS());
        ExecutionContext.put(S3Client.class, s3Client());

        warmUpSessionPool();
    }

    private void warmUpSessionPool() {
        ExecutionContext.get(TransactionManager.class).executeInReadTransaction(() -> null);
    }

    private QldbDriver qldbDriver() {
        return QldbDriver.builder()
                .ledger(LambdaEnv.getSlglQldbLedger())
                .maxConcurrentTransactions(3)
                .transactionRetryPolicy(RetryPolicy.builder()
                        .maxRetries(0)
                        .build())
                .sessionClientBuilder(QldbSessionClient.builder())
                .build();
    }

    private DynamoDbClient amazonDynamoDB() {
        return DynamoDbClient.create();
    }

    private SqsClient amazonSQS() {
        return SqsClient.create();
    }

    private S3Client s3Client() {
        return S3Client.create();
    }
}
