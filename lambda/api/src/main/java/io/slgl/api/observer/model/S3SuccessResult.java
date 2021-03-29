package io.slgl.api.observer.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class S3SuccessResult implements SuccessResult {

    private final String bucketName;
    private final String key;
    private final String versionId;
    private final String eTag;

    public S3SuccessResult(String bucketName, String key, String versionId, String eTag) {
        this.bucketName = bucketName;
        this.key = key;
        this.versionId = versionId;
        this.eTag = eTag;
    }

    @Override
    public String getMessage() {
        return String.format("Package successfully uploaded to S3 bucket. Bucket: %s, key: %s, versionId: %s, eTag: %s",
                bucketName, key, versionId, eTag);
    }

    @Override
    public Result log() {
        log.info(getMessage());
        return this;
    }

    @Override
    public Result log(String msg) {
        log.info(String.format("%s: %s", msg, getMessage()));
        return this;
    }
}
