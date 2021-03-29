package io.slgl.api.observer.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

import javax.validation.constraints.NotEmpty;

@Accessors(chain = true)
@Getter
@Setter
public class S3Credentials {

    @NotEmpty
    private String accessKeyId;

    @NotEmpty
    private String secretAccessKey;

    public AwsCredentialsProvider createAwsCredentials() {
        var credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        return StaticCredentialsProvider.create(credentials);
    }
}
