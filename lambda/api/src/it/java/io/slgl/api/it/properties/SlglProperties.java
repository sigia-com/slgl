package io.slgl.api.it.properties;

import static java.util.Objects.requireNonNull;

public class SlglProperties {

    private final PropertyLoader loader;

    SlglProperties(PropertyLoader loader) {
        this.loader = loader;
    }

    private String get(String propertyKey, String defaultValue) {
        return requireNonNull(loader.getString(propertyKey, defaultValue));
    }

    private String get(String propertyKey) {
        return requireNonNull(loader.getString(propertyKey));
    }

    public String getLedgerUrl() {
        return get("ledger.url");
    }

    public String getAdminUsername() {
        return get("ledger.admin.username", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    }

    public String getAdminApiKey() {
        return get("ledger.admin.apiKey");
    }

    public String getObserverDeadLetterBucket() {
        return get("ledger.observerDeadLetter.s3.bucket");
    }

    public String getObserverDeadLetterRegion() {
        return get("ledger.observerDeadLetter.s3.region");
    }

    public String getAuditorSqsQueueUrl() {
        return get("ledger.auditor.sqs.url");
    }

    public String getObserverStorageS3Bucket() {
        return get("ledger.observerStorage.s3.bucket");
    }

    public String getObserverStorageS3Region() {
        return get("ledger.observerStorage.s3.region", getObserverDeadLetterRegion());
    }

    public String getStateStorageS3Bucket() {
        return get("ledger.stateStorage.s3.bucket");
    }

    public String getStateStorageS3Region() {
        return get("ledger.stateStorage.s3.region", getObserverDeadLetterRegion());
    }
}
