package io.slgl.api.it;

import io.slgl.api.it.data.PgpTestKeys;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.NodeRequest;
import io.slgl.client.node.NodeResponse;
import io.slgl.client.node.ObserverNodeRequest;
import io.slgl.client.storage.S3Storage;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static org.assertj.core.api.Assertions.assertThat;

public class ObserverCreationIT extends AbstractApiTest {

    @Test
    public void shouldCreateObserverNode() {
        //given
        String id = generateUniqueId();
        S3Storage storage = S3Storage.builder()
            .bucket("s3-bucket-name")
            .path("path-inside-basket")
            .region("eu-west-1")
            .credentials("key_id", "secret_key")
            .build();
        NodeRequest nodeRequest = NodeRequest.builder()
            .id(id)
            .linkObserver(ObserverNodeRequest.builder()
                .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                .recoveryStoragePath("observer-recovery-path")
                .s3Storage(storage))
            .build();

        //when
        NodeResponse nodeResponse = ledger.writeNode(nodeRequest);

        //then
        assertThat(nodeResponse.getId()).isEqualTo(id);
    }

    @Test
    public void shouldFailWhenBucketIsMissing() {
        //given
        NodeRequest nodeRequest = NodeRequest.builder()
            .id(generateUniqueId())
            .linkObserver(ObserverNodeRequest.builder()
                .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                .recoveryStoragePath("observer-recovery-path")
                .s3Storage(S3Storage.builder()
                    .path("path-inside-bucket")
                    .region("eu-west-1")
                    .credentials("key_id", "secret_key")))
            .build();

        //when
        ErrorResponse errorResponse = expectError(() -> ledger.writeNode(nodeRequest));

        //then
        assertThat(errorResponse.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
    }

    @Test
    public void shouldFailWhenPathIsMissing() {
        //given
        NodeRequest nodeRequest = NodeRequest.builder()
            .id(generateUniqueId())
            .linkObserver(ObserverNodeRequest.builder()
                .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                .recoveryStoragePath("observer-recovery-path")
                .s3Storage(S3Storage.builder()
                    .bucket("bucket-name")
                    .region("eu-west-1")
                    .credentials("key_id", "secret_key")))
            .build();

        //when
        ErrorResponse errorResponse = expectError(() -> ledger.writeNode(nodeRequest));

        //then
        assertThat(errorResponse.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
    }

    @Test
    public void shouldAcceptObserverRequestWithEmptyCredentials() {
        //given
        NodeRequest nodeRequest = NodeRequest.builder()
            .id(generateUniqueId())
            .linkObserver(ObserverNodeRequest.builder()
                .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                .recoveryStoragePath("observer-recovery-path")
                .s3Storage(S3Storage.builder()
                    .bucket("bucket-name")
                    .path("path-in-basket")
                    .region("eu-west-1")))
            .build();

        //when
        NodeResponse nodeResponse = ledger.writeNode(nodeRequest);

        //then
        assertThat(nodeResponse.getId()).isEqualTo(nodeRequest.getId());
    }

    @Test
    public void shouldFailWhenCredentialsKeyIdIsMissing() {
        //given
        NodeRequest nodeRequest = NodeRequest.builder()
            .id(generateUniqueId())
            .linkObserver(ObserverNodeRequest.builder()
                .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                .recoveryStoragePath("observer-recovery-path")
                .s3Storage(S3Storage.builder()
                    .bucket("bucket-name")
                    .path("path-in-basket")
                    .region("eu-west-1")
                .credentials(null, "secret-key")))
            .build();

        //when
        ErrorResponse errorResponse = expectError(() -> ledger.writeNode(nodeRequest));

        //then
        assertThat(errorResponse.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
    }

    @Test
    public void shouldFailWhenCredentialsAccessKeyIsMissing() {
        //given
        NodeRequest nodeRequest = NodeRequest.builder()
            .id(generateUniqueId())
            .linkObserver(ObserverNodeRequest.builder()
                .pgpKey(PgpTestKeys.JON_DOE_PUBLIC_KEY)
                .recoveryStoragePath("observer-recovery-path")
                .s3Storage(S3Storage.builder()
                    .bucket("bucket-name")
                    .path("path-in-basket")
                    .region("eu-west-1")
                    .credentials("keyId", null)))
            .build();

        //when
        ErrorResponse errorResponse = expectError(() -> ledger.writeNode(nodeRequest));

        //then
        assertThat(errorResponse.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
    }
}
