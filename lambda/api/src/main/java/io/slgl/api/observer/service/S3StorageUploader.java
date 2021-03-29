package io.slgl.api.observer.service;

import com.google.common.base.Preconditions;
import io.slgl.api.observer.model.ObserverData;
import io.slgl.api.observer.model.Result;
import io.slgl.api.observer.model.S3Credentials;
import io.slgl.api.observer.model.S3Storage;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import static java.util.Optional.ofNullable;

@Slf4j
public class S3StorageUploader extends AbstractS3StorageUploader {

    private final S3Storage storage;

    S3StorageUploader(S3Storage storage, PgpEncrypter pgpEncrypter) {
        super(pgpEncrypter);
        Preconditions.checkNotNull(storage);
        this.storage = storage;
    }

    @Override
    public Result upload(ObserverData observerData, String pgpPublicKey) {
        return uploadData(
            createClient(storage.getCredentials()),
            observerData,
            pgpPublicKey,
            storage.getBucket(),
            buildPath(storage.getPath(), observerData)
        );
    }

    private S3Client createClient(S3Credentials credentials) {
        return ofNullable(credentials)
                .map(this::createClientWithCredentials)
                .orElseGet(this::createDefaultClient);
    }


    private S3Client createClientWithCredentials(S3Credentials s3Credentials) {
        return S3Client.builder()
                .region(getRegion())
                .credentialsProvider(s3Credentials.createAwsCredentials())
                .build();
    }

    private S3Client createDefaultClient() {
        return S3Client.builder()
                .region(getRegion())
                .build();
    }

    private Region getRegion() {
        return Region.of(storage.getRegion());
    }
}
