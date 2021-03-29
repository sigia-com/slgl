package io.slgl.client.storage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class S3Storage {

    @JsonProperty("region")
    private String region;

    @JsonProperty("bucket")
    private String bucket;

    @JsonProperty("path")
    private String path;

    @JsonProperty("credentials")
    private S3Credentials credentials;

    private S3Storage(Builder builder) {
        this.region = builder.region;
        this.bucket = builder.bucket;
        this.path = builder.path;
        this.credentials = builder.credentials;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        S3Storage s3Storage = (S3Storage) o;
        return Objects.equals(region, s3Storage.region) &&
                Objects.equals(bucket, s3Storage.bucket) &&
                Objects.equals(path, s3Storage.path) &&
                Objects.equals(credentials, s3Storage.credentials);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, bucket, path, credentials);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String region;
        private String bucket;
        private String path;
        private S3Credentials credentials;

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder credentials(String keyId, String secretKey) {
            this.credentials = new S3Credentials(keyId, secretKey);
            return this;
        }

        public S3Storage build() {
            return new S3Storage(this);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class S3Credentials {

        @JsonProperty("access_key_id")
        private String accessKeyId;

        @JsonProperty("secret_access_key")
        private String secretAccessKey;

        public S3Credentials(String accessKeyId, String secretAccessKey) {
            this.accessKeyId = accessKeyId;
            this.secretAccessKey = secretAccessKey;
        }
    }
}
