package io.slgl.api.it.properties;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toUnmodifiableMap;

@Slf4j
class AwsStackPropertyLoader implements PropertyLoader {

    private static final Map<String, String> propertyToStackOutputName = Map.of(
            "ledger.url", "ApiUrl",
            "ledger.admin.apiKey", "DefaultAdminApiKey",
            "ledger.observerDeadLetter.s3.bucket", "ObserverDeadLetterBucket",
            "ledger.observerDeadLetter.s3.region", "ObserverDeadLetterBucketRegion",
            "ledger.auditor.sqs.url", "TestAuditorSqsQueue",
            "ledger.observerStorage.s3.bucket", "TestObserverStorageS3Bucket",
            "ledger.stateStorage.s3.bucket", "TestStateStorageS3Bucket"
    );

    private final Map<String, String> stackOutputs = loadStackProperties();

    private Map<String, String> loadStackProperties() {
        String devLedgerStackName = readStackName();
        if (StringUtils.isBlank(devLedgerStackName)) {
            return Collections.emptyMap();
        }
        var stackOutputs = loadStackOutputs(devLedgerStackName);
        log.info("Configuration loaded from stack: stackName={}, stackOutputs={}",
                devLedgerStackName, stackOutputs);
        return stackOutputs;
    }

    private String readStackName() {
        var propertyLoader = new PropertiesFilePropertyLoader("/etc/slgl/bootstrap.properties", "io.slgl.it.bootstrap.properties");
        return System.getProperty("dev.slgl.stackName", propertyLoader.getString("dev.slgl.stackName"));
    }

    private Map<String, String> loadStackOutputs(String devLedgerStackName) {
        setupAwsCredentialsProfile();
        var client = AmazonCloudFormationClientBuilder.defaultClient();
        var describeResult = client.describeStacks(
                new DescribeStacksRequest().withStackName(devLedgerStackName));
        var stacks = describeResult.getStacks();
        checkState(stacks.size() == 1, "expected exactly 1 stack of name '%s', got: %d", devLedgerStackName, stacks.size());

        return stacks.get(0)
                .getOutputs()
                .stream()
                .collect(toUnmodifiableMap(Output::getOutputKey, Output::getOutputValue));
    }

    private void setupAwsCredentialsProfile() {
        if (System.getProperty("aws.profile") == null) {
            System.setProperty("aws.profile", "slgl");
        }
    }

    @Override
    public String getString(String property) {
        var stackOutputName = propertyToStackOutputName.getOrDefault(property, property);
        return stackOutputs.get(stackOutputName);
    }
}
