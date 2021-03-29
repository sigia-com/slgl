package io.slgl.api.it;

import io.slgl.client.Types;
import io.slgl.client.camouflage.Camouflage;
import io.slgl.client.node.NodeRequest;
import io.slgl.client.node.NodeResponse;
import io.slgl.client.node.TypeNodeRequest;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowAllForEveryone;
import static io.slgl.api.it.utils.HashUtils.sha3_512;
import static io.slgl.client.node.NodeTypeId.simple;

public class UploadBinaryFileIT extends AbstractApiTest {

    @Test
    public void shouldUploadBinaryFile() {
        // given
        NodeRequest request = NodeRequest.builder()
                .file("some binary content".getBytes())
                .build();

        // when
        NodeResponse response = ledger.writeNode(request);

        // then
        assertThat(response.getId()).isNotEmpty();
        assertThat(response.getFileSha3()).isEqualTo(sha3_512(request.getFile()));
        assertThat(response.getObjectSha3()).isNull();
        assertThat(response.getStateSha3()).isNotNull();
        assertThat(response.getState()).containsKey("@file");
    }

    @Test
    public void shouldUploadBinaryFileWithCamouflaged() {
        // given
        NodeResponse type = user.writeNode(TypeNodeRequest.builder()
                .anchor("#anchor")
                .permission(allowAllForEveryone()));

        NodeRequest request = NodeRequest.builder()
                .type(type)
                .file("some binary content".getBytes())
                .camouflage(Camouflage.builder()
                        .anchor("#observersC", "#observers")
                        .anchor("#auditorsC", "#auditors")
                        .anchor("#anchorC", "#anchor"))
                .build();

        // when
        NodeResponse response = user.writeNode(request);

        // then
        assertThat(response.getType()).isEqualTo(simple(Types.CAMOUFLAGE));
        assertThat(response.getCamouflageSha3()).isNotNull();
        assertThat(response.getFileSha3()).isEqualTo(sha3_512(request.getFile()));
        assertThat(response.getObjectSha3()).isNull();
    }
}
