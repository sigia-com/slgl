package io.slgl.api.it;

import io.slgl.api.it.data.PdfMother;
import io.slgl.client.node.NodeRequest;
import io.slgl.client.node.TypeNodeRequest;
import io.slgl.client.node.WriteRequest;
import io.slgl.client.node.permission.Permission;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.slgl.api.it.data.PermissionMother.allowReadStateForEveryone;
import static io.slgl.client.node.permission.Allow.allowLink;
import static io.slgl.client.node.permission.Requirement.requireThat;

public class PdfMetadataIT extends AbstractApiTest {

    @Test
    @SneakyThrows
    void shouldProvideAllPdfMetadataInFileContextRegardlessOfType() {
        // given
        var linkRequirements = List.of(
                requireThat("$source_node.$file.pdf_info.Title").isEqualTo().value("value"),
                requireThat("$source_node.$file.pdf_info.text").isEqualTo().value("value"),
                requireThat("$source_node.$file.pdf_info.number").isEqualTo().value(1),
                requireThat("$source_node.$file.pdf_info.boolean").isEqualTo().value(true),
                requireThat("$source_node.$file.pdf_info.map.nested_text").isEqualTo().value("nested value")
        );
        var pdfInfo = Map.of(
                "Title", "value",
                "text", "value",
                "number", 1,
                "boolean", true,
                "map", Map.of(
                        "nested_text", "nested value"
                )
        );

        var targetNodeRequest = NodeRequest.builder()
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .requireAll(linkRequirements)))
                .build();

        var fileRequest = NodeRequest.builder()
                .type(TypeNodeRequest.builder().permission(allowReadStateForEveryone()))
                .file(PdfMother.createSignedPdf("TEST PDF TEXT", null, pdfInfo))
                .build();

        // when
        var targetNode = user.writeNode(targetNodeRequest);

        // when
        Assertions.assertThatCode(() -> {
            user.write(WriteRequest.builder()
                    .addRequest(fileRequest)
                    .addLinkRequest(0, targetNode, "#anchor")
            );
        }).doesNotThrowAnyException();

    }
}
