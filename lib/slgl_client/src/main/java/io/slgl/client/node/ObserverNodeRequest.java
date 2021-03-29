package io.slgl.client.node;

import io.slgl.client.Types;
import io.slgl.client.storage.RecoveryStorage;
import io.slgl.client.storage.S3Storage;

public final class ObserverNodeRequest extends NodeRequest {

    private ObserverNodeRequest(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends NodeRequest.Builder<Builder> {

        {
            super.type(Types.OBSERVER);
        }

        public Builder pgpKey(String pgpKey) {
            this.data("pgp_key", pgpKey);
            return this;
        }

        public Builder s3Storage(S3Storage storage) {
            this.data("aws_s3", storage);
            return this;
        }

        public Builder s3Storage(S3Storage.Builder storage) {
            this.data("aws_s3", storage.build());
            return this;
        }

        public Builder recoveryStoragePath(String path) {
            this.data("recovery_storage", new RecoveryStorage(path));
            return this;
        }

        @Override
        public ObserverNodeRequest build() {
            return new ObserverNodeRequest(this);
        }
    }


}
