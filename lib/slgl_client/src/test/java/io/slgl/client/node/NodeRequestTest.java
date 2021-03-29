package io.slgl.client.node;

import io.slgl.client.node.permission.Allow;
import io.slgl.client.node.permission.Permission;
import io.slgl.client.node.permission.Requirement;
import io.slgl.client.storage.S3Storage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeRequestTest {

    @Test
    public void shouldBeEqual() {

        assertThat(
                NodeRequest.builder()
                        .id("test")
                        .type(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("allow"))
                                        .requireAll(Requirement.requireThat("somepath").isEqualTo().value(""))
                                        .build()))
                        .data("foo", "bar")
                        .linkObserver(ObserverNodeRequest.builder()
                                .s3Storage(S3Storage.builder().path("path").bucket("dsad").region("test").build())
                                .pgpKey("some_key")
                                .build())
                        .build()
        )
                .isEqualTo(
                        NodeRequest.builder()
                                .id("test")
                                .type(TypeNodeRequest.builder()
                                        .permission(Permission.builder()
                                                .allow(Allow.allowLink("allow"))
                                                .requireAll(Requirement.requireThat("somepath").isEqualTo().value(""))
                                                .build()))
                                .data("foo", "bar")
                                .linkObserver(ObserverNodeRequest.builder()
                                        .s3Storage(S3Storage.builder().path("path").bucket("dsad").region("test").build())
                                        .pgpKey("some_key")
                                        .build())
                                .build()
                )
                .isNotEqualTo(NodeRequest.builder()
                        .id("test")
                        .type(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("allow"))
                                        .requireAll(Requirement.requireThat("somepath").isEqualTo().value(""))
                                        .build()))
                        .data("foo", "bar")
                        .linkObserver(ObserverNodeRequest.builder()
                                .s3Storage(S3Storage.builder().path("path").bucket("dsad").region("test").build())
                                .pgpKey("other_key")
                                .build())
                        .build()
                )
                .isNotEqualTo(NodeRequest.builder()
                        .id("test")
                        .type(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("allow"))
                                        .requireAll(Requirement.requireThat("somepath").isEqualTo().ref("ref"))
                                        .build()))
                        .data("foo", "bar")
                        .linkObserver(ObserverNodeRequest.builder()
                                .s3Storage(S3Storage.builder().path("path").bucket("dsad").region("test").build())
                                .pgpKey("some_key")
                                .build())
                        .build()
                );

    }

}
