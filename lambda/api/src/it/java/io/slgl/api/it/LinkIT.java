package io.slgl.api.it;

import io.slgl.api.it.data.PermissionMother;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.Types;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.slgl.api.it.assertj.SlglAssertions.assertApiException;
import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowAllForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.permission.Allow.allowLink;

public class LinkIT extends AbstractApiTest {

    @Test
    public void shouldLinkNode() {
        // given
        NodeResponse parentType = ledger.writeNode(
                TypeNodeRequest.builder()
                        .id(generateUniqueId())
                        .anchor("#child")
                        .permission(allowAllForEveryone()));

        NodeResponse parent = writeNode("parent", parentType);
        NodeResponse child = writeNode("child");

        WriteRequest linkRequest = WriteRequest.builder()
                .addLinkRequest(child, parent, "#child")
                .build();

        // when
        WriteResponse response = ledger.write(linkRequest);

        // then
        assertThat(response)
                .hasResponsesSize(1)
                .hasLink(child.getId(), parent.getId(), "#child");
    }

    @Test
    public void shouldLinkToNodeWithInlineType() {
        // given
        NodeResponse parent = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#child")
                        .permission(allowAllForEveryone())));

        NodeResponse child = writeNode("child");

        WriteRequest linkRequest = WriteRequest.builder()
                .addLinkRequest(child, parent, "#child")
                .build();

        // when
        WriteResponse response = ledger.write(linkRequest);

        // then
        assertThat(response)
                .hasResponsesSize(1)
                .hasLink(child.getId(), parent.getId(), "#child");
    }

    @Test
    public void shouldLinkToNodeWithAnchorWithType() {
        // given
        NodeResponse childType = createType("child-type");

        NodeResponse parentType = ledger.writeNode(
                TypeNodeRequest.builder()
                        .id(generateUniqueId())
                        .anchor("#child", childType.getId())
                        .permission(allowAllForEveryone())
                        .build());

        NodeResponse parent = writeNode("parent", parentType);
        NodeResponse child = writeNode("child", childType);

        WriteRequest linkRequest = WriteRequest.builder()
                .addLinkRequest(child, parent, "#child")
                .build();

        // when
        WriteResponse response = ledger.write(linkRequest);

        // then
        assertThat(response)
                .hasResponsesSize(1)
                .hasLink(child.getId(), parent.getId(), "#child");
    }

    @Test
    public void shouldLinkNodeCreatedInTheSameRequest() {
        // given
        NodeResponse parent = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#child")
                        .permission(allowAllForEveryone())));

        String childNodeId = generateUniqueId();

        WriteRequest request = WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .id(childNodeId))
                .addLinkRequest(childNodeId, parent.getId(), "#child")
                .build();

        // when
        WriteResponse response = ledger.write(request);

        // then
        assertThat(response)
                .hasResponsesSize(2)
                .hasNodeWithId(childNodeId)
                .hasLink(childNodeId, parent.getId(), "#child");

    }

    @Test
    public void shouldLinkNodeWithAutogeneratedIdCreatedInTheSameRequest() {
        // given
        NodeResponse parent = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#child")
                        .permission(allowAllForEveryone())));

        WriteRequest request = WriteRequest.builder()
                .addRequest(NodeRequest.builder().build())
                .addLinkRequest(0, parent, "#child")
                .build();

        // when
        WriteResponse response = ledger.write(request);

        // then
        assertThat(response)
                .hasResponsesSize(2)
                .hasLink(
                        ((NodeResponse) response.getResponses().get(0)).getId(),
                        parent.getId(),
                        "#child"
                );
    }

    @Test
    public void shouldCreateNodeWithNestedInlineLink() {
        // given
        NodeResponse parentType = ledger.writeNode(
                TypeNodeRequest.builder()
                        .id(generateUniqueId())
                        .anchor("#child")
                        .build());

        // when
        NodeResponse response = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(parentType.getId())
                .linkNode("#child", NodeRequest.builder()
                        .type(parentType.getId())
                        .data("inline-link", "level-1")
                        .linkNode("#child", NodeRequest.builder()
                                .data("inline-link", "level-2")))
                .build());

        // then
        assertThat(response)
                .isNotNull()
                .hasValueAtStatePath("level-1", "#child", 0, "inline-link")
                .hasValueAtStatePath("level-2", "#child", 0, "#child", 0, "inline-link");
    }

    @Test
    public void shouldFailWhenLinkingToNotDefinedAnchor() {
        // given
        NodeResponse parentType = createType("parent-type");

        NodeResponse parent = writeNode("parent", parentType);

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, parent, "#not-existing-anchor")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(linkRequest));

        // then
        assertThat(error)
                .hasErrorCode(ErrorCode.LINKING_TARGET_ANCHOR_NOT_FOUND)
                .messageContains("#not-existing-anchor");
    }


    @Test
    public void shouldFailWhenLinkingToNotDefinedAnchorInType() {
        // given
        NodeResponse type = createType("type");

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, type, "#not-existing-anchor")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(linkRequest));

        // then
        assertThat(error)
                .hasErrorCode(ErrorCode.LINKING_TARGET_ANCHOR_NOT_FOUND)
                .messageContains("#not-existing-anchor");
    }

    @Test
    public void shouldFailWhenLinkingToNotDefinedAnchorInTypeInlineType() {
        // given
        NodeResponse parent = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#child")
                        .permission(allowAllForEveryone())));

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, parent, "#not-existing-anchor")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(linkRequest));

        // then
        assertThat(error)
                .hasErrorCode(ErrorCode.LINKING_TARGET_ANCHOR_NOT_FOUND)
                .messageContains("#not-existing-anchor");
    }

    @Test
    public void shouldFailWhenAddingEntryWithInlineAnchorThatIsNotDefined() {
        // given
        NodeResponse parentType = createType("parent-type");

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(parentType.getId())
                .linkNode("#child", NodeRequest.builder()
                        .data("example-key", "example-value"))
                .build()));

        // then
        assertThat(error)
                .hasErrorCode(ErrorCode.LINKING_TARGET_ANCHOR_NOT_FOUND)
                .messageContains("#child");
    }

    @Test
    public void shouldLinkToAnchorThatHasInlineLinks() {
        // given
        NodeResponse parentType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#child")
                .permission(allowAllForEveryone())
                .build());

        NodeResponse parent = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(parentType)
                .linkNode("#child", NodeRequest.builder()
                        .data("example-key", "example-value"))
                .build());

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, parent, "#child")
                .build();

        // when
        WriteResponse linkResponse = ledger.write(linkRequest);

        // then
        assertThat(linkResponse)
                .isNotNull()
                .hasResponsesSize(2);
    }

    @Test
    public void shouldLinkToAnchorDefinedInBaseType() {
        // given
        NodeResponse node = writeNode("node");

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#fake")
                .build();

        // when
        WriteResponse response = ledger.write(linkRequest);

        // then
        assertThat(response)
                .isNotNull()
                .hasResponsesSize(2);
    }

    @Test
    public void shouldValidateThatLinkIsUnique() {
        // given
        NodeResponse parent = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#child")
                        .permission(allowAllForEveryone())));

        NodeResponse child = writeNode("child");

        WriteRequest request = WriteRequest.builder()
                .addLinkRequest(child, parent, "#child")
                .build();

        ledger.write(request);

        // when
        ErrorResponse error = expectError(() -> ledger.write(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.LINK_ALREADY_EXISTS);
    }

    @Test
    public void shouldFailWhenLinkingObjectWithNotMatchingType() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#anchor"));

        NodeResponse otherType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#other_anchor"));

        NodeResponse node = writeNode("node", TypeNodeRequest.builder()
                .anchor("#test", type)
                .permission(allowAllForEveryone()));

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(otherType))
                .addLinkRequest(0, node, "#test")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(linkRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.LINKING_NODE_TYPE_NOT_MATCHING_ANCHOR_TYPE);
    }

    @Test
    public void shouldFailWhenLinkingNotValidObject() {
        // given
        NodeResponse node = writeNode("node");

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(Types.OBSERVER)
                        .data("not-an-observer-property", "example"))
                .addLinkRequest(0, node, "#observers")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(linkRequest));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.UNABLE_TO_PARSE_UNRECOGNIZED_FIELD, "not-an-observer-property");
    }

    @Test
    public void shouldFailWhenCreatingEntryWithInlineLinkWithNotValidObject() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .linkNode("#observers", NodeRequest.builder()
                        .data("not-an-observer-property", "example"))
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.UNABLE_TO_PARSE_UNRECOGNIZED_FIELD, "not-an-observer-property");
    }

    @Test
    public void shouldFailWhenCreatingEntryWithInlineLinkThatIsNotList() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .data("#permissions", Permission.builder()
                        .allow(allowLink("#permissions"))
                        .build())
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.UNABLE_TO_PARSE_FIELD, "requests[0].#permissions");
    }

    @Test
    public void shouldFailWhenLinkingMoreEntriesThanMaxSize() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#test", 2)
                .permission(allowAllForEveryone()));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type));

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test")
                .build();

        ledger.write(linkRequest);
        ledger.write(linkRequest);

        // when
        ErrorResponse error = expectError(() -> ledger.write(linkRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldIncludeInlineLinksWhenCalculatingMaxSizeConstraint() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#test", 2)
                .permission(allowAllForEveryone()));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type)
                .linkNode("#test", NodeRequest.builder()));

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#test")
                .build();

        // when
        ledger.write(linkRequest);

        // then
        assertApiException(() -> ledger.write(linkRequest))
                .hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldFailWhenCreatingEntryWithMoreInlineLinksThanMaxSize() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#test", 2));

        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(type.getId())
                .linkNode("#test", NodeRequest.builder())
                .linkNode("#test", NodeRequest.builder())
                .linkNode("#test", NodeRequest.builder())
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldLinkNodeToItself() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permission(PermissionMother.allowAllForEveryone())));

        WriteRequest linkRequest = WriteRequest.builder()
                .addLinkRequest(node, node, "#anchor")
                .build();

        // when
        WriteResponse response = ledger.write(linkRequest);

        // then
        assertThat(response)
                .hasResponsesSize(1)
                .hasLink(node.getId(), node.getId(), "#anchor");
    }

    @Test
    public void shouldCreateLinksInBatch() {
        shouldCreateLinksInBatch(20);
    }

    @Test
    public void shouldReturnErrorWhenCreatingTooManyLinksInSingleBatch() {
        // when
        ErrorResponse error = expectError(() -> shouldCreateLinksInBatch(21));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.TOO_MANY_REQUESTS_IN_BATCH, 20);
    }

    private void shouldCreateLinksInBatch(int numberOfLinks) {
        // given
        WriteRequest.Builder nodesRequestBuilder = WriteRequest.builder();
        for (int i = 0; i < Math.round(Math.sqrt(numberOfLinks) + 0.5); i++) {
            nodesRequestBuilder.addRequest(NodeRequest.builder()
                    .id(generateUniqueId())
                    .type(TypeNodeRequest.builder()
                            .anchor("#anchor")
                            .permission(PermissionMother.allowAllForEveryone())));
        }

        List<NodeResponse> nodes = ledger.write(nodesRequestBuilder).getNodes();
        int sourceNodeIndex = 0;
        int targetNodeIndex = 0;

        WriteRequest.Builder linksRequestBuilder = WriteRequest.builder();
        for (int i = 0; i < numberOfLinks; i++) {
            linksRequestBuilder.addRequests(LinkRequest.builder()
                    .sourceNode(nodes.get(sourceNodeIndex))
                    .targetNode(nodes.get(targetNodeIndex))
                    .targetAnchor("#anchor"));

            sourceNodeIndex++;
            if (sourceNodeIndex >= nodes.size()) {
                sourceNodeIndex = 0;
                targetNodeIndex++;
            }
        }
        WriteRequest linksRequest = linksRequestBuilder.build();

        // when
        WriteResponse linksResponse = ledger.write(linksRequest);

        // then
        assertThat(linksResponse).hasResponsesSize(numberOfLinks);
    }

    @Test
    public void shouldReturnErrorWhenCreatingLinkWithInvalidData() {
        // given
        WriteRequest request = WriteRequest.builder()
                .addRequests(LinkRequest.builder()
                        .sourceNode("not_a_valid_url")
                        .targetNode("not_a_valid_url")
                        .targetAnchor("not_a_valid_anchor"))
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(request));

        // then
        softly.assertThat(error)
                .hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].source_node", "pattern")
                .hasFieldError("requests[0].target_node", "pattern")
                .hasFieldError("requests[0].target_anchor", "pattern");
    }

    @Test
    public void shouldReturnErrorWhenCreatingLinkWithInvalidSourceNodeReferences() {
        // given
        WriteRequest request = WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .anchor("#anchor")
                                .permission(allowAllForEveryone())))
                .addRequest(NodeRequest.builder())
                .addLinkRequest(1, 0, "#anchor")
                .addLinkRequest(999, 0, "#anchor")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(request));

        // then
        assertThat(error)
                .hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[3].source_node", "valid_node_request_refs");
    }

    @Test
    public void shouldReturnErrorWhenCreatingLinkWithInvalidTargetNodeReferences() {
        // given
        WriteRequest request = WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .anchor("#anchor")
                                .permission(allowAllForEveryone())))
                .addRequest(NodeRequest.builder())
                .addLinkRequest(1, 0, "#anchor")
                .addLinkRequest(1, 999, "#anchor")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(request));

        // then
        assertThat(error)
                .hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[3].target_node", "valid_node_request_refs");
    }
}