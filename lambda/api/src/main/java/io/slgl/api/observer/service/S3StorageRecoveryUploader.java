package io.slgl.api.observer.service;

import com.google.common.base.Preconditions;
import io.slgl.api.observer.model.ObserverData;
import io.slgl.api.observer.model.Result;
import io.slgl.api.utils.LambdaEnv;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3StorageRecoveryUploader extends AbstractS3StorageUploader {

    private final String path;

    S3StorageRecoveryUploader(String path, PgpEncrypter pgpEncrypter) {
        super(pgpEncrypter);
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    @Override
    public Result upload(ObserverData observerData, String pgpPublicKey) {
        return uploadData(
                createRecoveryClient(),
                observerData,
                pgpPublicKey,
                getRecoveryStorageBucketName(),
                buildPath(path, observerData)
        );
    }

    private S3Client createRecoveryClient() {
        return S3Client.builder()
                .region(getRecoveryStorageRegion())
                .build();
    }

    private Region getRecoveryStorageRegion() {
        return Region.of(LambdaEnv.S3ObserverRecovery.getStorageRegion());
    }

    private String getRecoveryStorageBucketName() {
        return LambdaEnv.S3ObserverRecovery.getStorageBucket();
    }
}
