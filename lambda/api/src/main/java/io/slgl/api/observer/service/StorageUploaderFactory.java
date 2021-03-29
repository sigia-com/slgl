package io.slgl.api.observer.service;

import io.slgl.api.ExecutionContext;
import io.slgl.api.observer.model.S3Storage;
import io.slgl.api.observer.model.Storage;

public class StorageUploaderFactory {

    private final PgpEncrypter pgpEncrypter = ExecutionContext.get(PgpEncrypter.class);

    public StorageUploader createUploader(Storage storage) {
        if (storage instanceof S3Storage) {
            return createS3StorageService((S3Storage) storage, pgpEncrypter);
        }
        throw new IllegalStateException(
            String.format("Unknown storage type: %s", storage.getClass().getCanonicalName()));
    }

    public StorageUploader createRecoveryUploader(String path) {
        return new S3StorageRecoveryUploader(path, pgpEncrypter);
    }

    private StorageUploader createS3StorageService(S3Storage storage, PgpEncrypter pgpEncrypter) {
        return new S3StorageUploader(storage, pgpEncrypter);
    }
}
