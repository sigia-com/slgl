package io.slgl.api.it;

import io.slgl.api.it.data.PermissionMother;
import io.slgl.api.it.user.StateStorage;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.Types;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowAllForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.permission.Allow.allowAll;
import static io.slgl.client.node.permission.Allow.allowLink;
import static io.slgl.client.node.permission.Requirement.requireThat;

public class TypeIT extends AbstractApiTest {

    @Test
    public void shouldAddNewType() {
        // given
        NodeRequest request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .build();

        // when
        NodeResponse response = ledger.writeNode(request);

        // then
        assertThat(response.getId()).isEqualTo(request.getId());
    }

    @Test
    public void shouldCreateNodeWithType() {
        // given
        NodeResponse type = createType();

        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(type.getId())
                .data("title", "example-title")
                .data("description", "example-description")
                .build();

        // when
        NodeResponse response = ledger.writeNode(request);

        // then
        assertThat(response.getId()).isEqualTo(request.getId());
        assertThat(response.getType()).isEqualTo(NodeTypeId.simple(type.getId()));
    }

    @Test
    public void shouldFailWhenAddingEntryWithTypeThatIsNotAType() {
        // given
        NodeResponse notTypeEntry = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId()));

        NodeRequest.Builder<?> request = NodeRequest.builder().id(generateUniqueId()).type(notTypeEntry.getId());

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.TYPE_IS_NOT_TYPE);
    }

    @Test
    public void shouldCreateNodeWithInlineType() {
        // given
        NodeRequest.Builder<?> request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .type((String) null)
                        .anchor("#child")
                        .permission(allowAllForEveryone()));

        // when
        NodeResponse node = ledger.writeNode(request);

        // then
        assertThat(node).isNotNull();
        assertThat(node.getType()).isNull();

        WriteResponse linkResponse = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#child"));
        assertThat(linkResponse).isNotNull();
    }

    @Test
    public void shouldCreateNodeWithInlineTypeThatExtendsOtherType() {
        // given
        NodeResponse baseType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId()));

        NodeRequest.Builder<?> request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .extendsType(baseType));

        // when
        NodeResponse entry = ledger.writeNode(request);

        // then
        assertThat(entry).isNotNull();
        assertThat(entry.getType()).isEqualTo(NodeTypeId.extended(baseType.getId()));

        NodeResponse readResponse = ledger.readNode(entry.getId(), ReadState.NO_STATE);
        assertThat(readResponse.getType()).isEqualTo(NodeTypeId.extended(baseType.getId()));
    }

    @Test
    public void shouldCreateLinkWithInlineType() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .type((String) null)
                        .anchor("#child")
                        .permission(allowAllForEveryone())));

        WriteRequest request = WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .type((String) null)
                                .anchor("#second_child")
                                .permission(allowAllForEveryone())))
                .addLinkRequest(0, node, "#child")
                .build();

        // when
        WriteResponse linkResponse = ledger.write(request);

        // then
        assertThat(linkResponse).isNotNull();

        WriteResponse nestedLinkResponse = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, linkResponse.getNodes().get(0), "#second_child"));
        assertThat(nestedLinkResponse).isNotNull();
    }

    @Test
    public void shouldCreateTypeWithAnchorWithInlineType() {
        // given
        TypeNodeRequest.Builder typeRequest = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#anchor", TypeNodeRequest.builder()
                        .stateProperties("title", "description"));

        // when
        NodeResponse type = ledger.writeNode(typeRequest);

        // then
        assertThat(type).isNotNull();
    }

    @Test
    public void shouldFailWhenCreatingTypeWithAnchorWithInvalidInlineType() {
        // given
        TypeNodeRequest.Builder request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#anchor", TypeNodeRequest.builder()
                        .data("not-a-property-of-type", "some-value"));

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.UNABLE_TO_PARSE_UNRECOGNIZED_FIELD, "anchors[0].type.not-a-property-of-type");
    }

    @Test
    public void shouldFailWhenCreatingEntryWithInlineLinkWithInvalidInlineType() {
        // given
        NodeRequest.Builder<?> request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor"))
                .linkNode("#anchor", NodeRequest.builder().type(TypeNodeRequest.builder()
                        .data("not-a-property-of-type", "some-value")));

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.UNABLE_TO_PARSE_UNRECOGNIZED_FIELD, "requests[0].#anchor[0].@type.not-a-property-of-type");
    }

    @Test
    public void shouldOverrideAnchorDefinitionFromBaseType() {
        // given
        NodeResponse baseType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#anchor")
                .permission(allowAllForEveryone()));

        // when
        NodeResponse extendedType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .extendsType(baseType.getId())
                .anchor("#anchor", 1));

        // then
        NodeResponse node = writeNode("node", extendedType);

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor")
                .build();

        WriteResponse linkResponse = ledger.write(linkRequest);
        assertThat(linkResponse).isNotNull();

        ErrorResponse error = expectError(() -> ledger.write(linkRequest));
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldCreateInlineTypeWithNestedInlineType() {
        // given
        TypeNodeRequest nestedTypeRequest = TypeNodeRequest.builder()
                .anchor("#second_child")
                .permission(allowAllForEveryone())
                .build();

        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#first_child", nestedTypeRequest)
                        .permission(allowAllForEveryone())
                        .build())
                .build();

        // when
        NodeResponse node = ledger.writeNode(request);

        // then
        assertThat(node).isNotNull();

        WriteResponse firstChildResponse = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(nestedTypeRequest))
                .addLinkRequest(0, node, "#first_child"));
        assertThat(firstChildResponse).isNotNull();

        WriteResponse secondChildResponse = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, firstChildResponse.getNodes().get(0), "#second_child"));
        assertThat(secondChildResponse).isNotNull();
    }

    private NodeResponse createType() {
        TypeNodeRequest.Builder request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("title", "description");

        return ledger.writeNode(request);
    }

    private NodeResponse createTypeWithAnchor(NodeResponse anchorType) {
        return ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#anchor", anchorType.getId()));
    }

    @Test
    public void shouldCreateTypeWithPermissionToReadState() {
        // given
        TypeNodeRequest typeRequest = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .extendsType(Types.TYPE)
                        .permission(PermissionMother.allowReadStateForEveryone()))
                .stateProperties("value")
                .build();

        // when
        NodeResponse typeNode = ledger.writeNode(typeRequest);

        // then
        NodeResponse nodeWithState = ledger.readNode(typeNode.getId(), ReadState.WITH_STATE);
        assertThat(nodeWithState).isNotNull();
        assertThat(nodeWithState.getState()).isNotEmpty();

        NodeResponse nodeWithType = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(typeNode));
        assertThat(nodeWithType).isNotNull();
        assertThat(nodeWithType.getType()).isEqualTo(NodeTypeId.simple(typeNode.getId()));
    }

    @Test
    public void shouldCreateTypeWithPermissionToReadStateUsingBuiltInType() {
        // given
        TypeNodeRequest typeRequest = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .publicType()
                .stateProperties("value")
                .build();

        // when
        NodeResponse typeNode = ledger.writeNode(typeRequest);

        // then
        NodeResponse nodeWithState = ledger.readNode(typeNode.getId(), ReadState.WITH_STATE);
        assertThat(nodeWithState).isNotNull();
        assertThat(nodeWithState.getState()).isNotEmpty();

        NodeResponse nodeWithType = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(typeNode));
        assertThat(nodeWithType).isNotNull();
        assertThat(nodeWithType.getType()).isEqualTo(NodeTypeId.simple(typeNode.getId()));
    }

    @Test
    public void shouldReturnErrorWhenCreatingTypeThatRelaxesPermissionFromBaseType() {
        // given
        NodeResponse baseType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#anchor")
                .anchor("#base_anchor")
                .permission(Permission.builder()
                        .allow(allowLink("#anchor"), allowLink("#base_anchor"))
                        .requireAll(requireThat("$node.value").isEqualTo().value("allow"))));

        TypeNodeRequest.Builder request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .extendsType(baseType)
                .anchor("#extended_anchor")
                .permission(Permission.builder()
                        .allow(allowLink("#anchor"), allowLink("#extended_anchor"))
                        .alwaysAllowed());

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].permissions", "valid_type")
                .fieldErrorContainsMessage("requests[0].permissions", "#anchor");
    }

    @Test
    public void shouldLinkToAnchorWithNodeWhoseTypeExtendsAnchorType() {
        // given
        NodeResponse baseType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#base_anchor")
                .stateProperties("base_value"));

        NodeResponse extendedType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .extendsType(baseType)
                .anchor("#extended_anchor")
                .stateProperties("extended_value"));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#test", baseType)
                        .permission(allowAllForEveryone())));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(extendedType))
                .addLinkRequest(0, node, "#test"));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldLinkToAnchorWithInlineTypeWithNodeWhoseTypeExtendsAnchorType() {
        // given
        TypeNodeRequest baseTypeRequest = TypeNodeRequest.builder()
                .anchor("#base_anchor")
                .stateProperties("base_value")
                .build();

        TypeNodeRequest extendedTypeRequest = TypeNodeRequest.builder()
                .anchor("#base_anchor")
                .anchor("#extended_anchor")
                .stateProperties("base_value", "extended_value")
                .build();

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#test", baseTypeRequest)
                        .permission(allowAllForEveryone())));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(extendedTypeRequest))
                .addLinkRequest(0, node, "#test"));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldReturnErrorWhenLinkToAnchorWithInlineTypeWithNodeWhoseTypeDoesNotExtendAnchorType() {
        // given
        TypeNodeRequest anchorTypeRequest = TypeNodeRequest.builder()
                .anchor("#anchor")
                .build();

        TypeNodeRequest otherTypeRequest = TypeNodeRequest.builder()
                .anchor("#other_anchor")
                .build();

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#test", anchorTypeRequest)
                        .permission(allowAllForEveryone())));

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(otherTypeRequest))
                .addLinkRequest(0, node, "#test")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.LINKING_NODE_TYPE_NOT_MATCHING_ANCHOR_TYPE);
    }

    @Test
    public void shouldExtendAnchorTypeWhenExtendingType() {
        // given
        NodeResponse baseAnchorType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#base_anchor"));

        NodeResponse baseType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#anchor", baseAnchorType));

        NodeResponse extendedAnchorType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .extendsType(baseAnchorType)
                .anchor("#extended_anchor"));

        // when
        NodeResponse extendedType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .extendsType(baseType)
                .anchor("#anchor", extendedAnchorType));

        // then
        assertThat(extendedType).isNotNull();

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(extendedType)
                .linkNode("#anchor", NodeRequest.builder()
                        .linkNode("#base_anchor", NodeRequest.builder())
                        .linkNode("#extended_anchor", NodeRequest.builder())));
        assertThat(node).isNotNull();
    }

    @Test
    public void shouldCreateTypeThatExtendsInlineTypeOfAnchor() {
        // given
        NodeResponse baseType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#anchor", TypeNodeRequest.builder()
                        .anchor("#base_anchor")));

        // when
        NodeResponse extendedType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .extendsType(baseType)
                .anchor("#anchor", TypeNodeRequest.builder()
                        .anchor("#base_anchor")
                        .anchor("#extended_anchor")));

        // then
        assertThat(extendedType).isNotNull();

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(extendedType)
                .linkNode("#anchor", NodeRequest.builder()
                        .linkNode("#base_anchor", NodeRequest.builder())
                        .linkNode("#extended_anchor", NodeRequest.builder())));
        assertThat(node).isNotNull();
    }

    @Test
    public void shouldReturnErrorWhenOverridingAnchorTypeWhenExtendingType() {
        // given
        NodeResponse baseAnchorType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#base_anchor"));

        NodeResponse baseType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#anchor", baseAnchorType));

        NodeResponse otherAnchorType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#extended_anchor"));

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .extendsType(baseType)
                .anchor("#anchor", otherAnchorType)));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    public void shouldAllowToExtendTypeInInlineTypeOfNodeCreatedInTheSameRequest() {
        // when
        var superType = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .build();

        var subtype = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .extendsType(superType.getId())
                        .anchor("#added_anchor")
                        .stateProperties("added_property")
                        .permission(Permission.builder()
                                .allow(allowAll())
                                .requireAll(requireThat("$node.ignores_requirement").isEqualTo().value("1")))
                )
                .build();

        // then
        ledger.write(WriteRequest.builder()
                .addRequest(superType)
                .addRequest(subtype));
    }

    @Test
    public void shouldReturnErrorWhenUsingTypeWithoutProvidingState() {
        // given
        String typeId = generateUniqueId();
        StateStorage.ignoreStateStorageForNode(typeId);

        NodeResponse type = user.writeNode(TypeNodeRequest.builder()
                .id(typeId)
                .stateProperties("foo", "bar"));

        // when
        ErrorResponse error = expectError(() -> user.writeNode(NodeRequest.builder()
                .type(type)));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.STATE_REQUIRED, type.getId());
    }

    @Test
    public void shouldValidateTypeObject() {
        // given
        var request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor(tooLongAnchor())
                .anchor("invalid-anchor")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].anchors[0].id", "length")
                .hasFieldError("requests[0].anchors[1].id", "pattern");
    }

    @Test
    public void shouldValidateInlineTypeObject() {
        // given
        var request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor(tooLongAnchor())
                        .anchor("invalid-anchor"))
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].@type.anchors[0].id", "length")
                .hasFieldError("requests[0].@type.anchors[1].id", "pattern");
    }

    @Test
    public void shouldValidateTypeObjectInInlineLink() {
        // given
        var request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#test"))
                .linkNode("#test", NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .anchor(tooLongAnchor())
                                .anchor("invalid-anchor")))
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].#test[0].@type.anchors[0].id", "length")
                .hasFieldError("requests[0].#test[0].@type.anchors[1].id", "pattern");
    }

    private static String tooLongAnchor() {
        String anchorId = "#anchor";
        anchorId += StringUtils.repeat("x", 101 - anchorId.length());

        return anchorId;
    }
}
