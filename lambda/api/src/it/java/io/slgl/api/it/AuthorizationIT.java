package io.slgl.api.it;

import com.google.common.collect.ImmutableList;
import io.slgl.api.it.data.PdfMother;
import io.slgl.api.it.utils.MapUtils;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.Types;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.jsonlogic.Equal;
import io.slgl.client.jsonlogic.Var;
import io.slgl.client.node.*;
import io.slgl.client.node.authorization.AuthorizationNodeRequest;
import io.slgl.client.node.authorization.Authorize;
import io.slgl.client.node.authorization.AuthorizeAction;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowReadStateForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.authorization.Authorize.authorizeLink;
import static io.slgl.client.node.authorization.Authorize.authorizeReadState;
import static io.slgl.client.node.permission.Allow.allowLink;
import static io.slgl.client.node.permission.Allow.allowReadState;
import static io.slgl.client.node.permission.Requirement.requireThat;
import static org.assertj.core.api.InstanceOfAssertFactories.map;

public class AuthorizationIT extends AbstractApiTest {

    @Test
    public void shouldCreateAuthorizationFromApiRequest() {
        // given
        AuthorizationNodeRequest request = AuthorizationNodeRequest.builder()
                .id(generateUniqueId())
                .authorize(authorizeLink(generateUniqueId(), "#anchor"))
                .requireAll(requireThat("$source_node.value").isEqualTo().value("allow"))
                .requireLogic(new Equal(new Var("$source_node.value"), "allow"))
                .build();

        // when
        NodeResponse node = user.writeNode(request);

        // then
        assertThat(node.getType()).isEqualTo(NodeTypeId.simple(Types.AUTHORIZATION));
        assertThat(node.getState()).containsKeys("authorize", "require", "require_logic", "authorization_principals");

        assertThat(MapUtils.get(node.getState(), "authorization_principals", 0, "api", "username"))
                .isNotNull()
                .isEqualTo(user.getUsername());
    }

    @Test
    public void shouldUseApiAuthorizationWhenLinkingNode() {
        // given
        NodeResponse testType = user.writeNode(TypeNodeRequest.builder()
                .stateProperties("value")
                .permission(allowReadStateForEveryone()));

        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                        )
                                ))));

        NodeResponse authorization = user.writeNode(AuthorizationNodeRequest.builder()
                .id(generateUniqueId())
                .authorize(authorizeLink(testNode, "#test"))
                .requireAll(requireThat("$source_node.value").isEqualTo().value("allow")));

        // when
        WriteResponse response = getSecondUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(testType)
                        .data("value", "allow"))
                .addRequest(LinkRequest.builder()
                        .sourceNode(0)
                        .targetNode(testNode)
                        .targetAnchor("#test")
                        .authorization(authorization)));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> getSecondUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(testType)
                        .data("value", "deny"))
                .addRequest(LinkRequest.builder()
                        .sourceNode(0)
                        .targetNode(testNode)
                        .targetAnchor("#test")
                        .authorization(authorization))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.AUTHORIZATION_PERMISSION_DENIED);

        // when
        error = expectError(() -> getSecondUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, testNode, "#test")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldUseApiAuthorizationWhenReadingState() {
        // given
        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .permission(Permission.builder()
                                .allow(allowReadState())
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                        )
                                ))));

        NodeResponse authorization = user.writeNode(AuthorizationNodeRequest.builder()
                .id(generateUniqueId())
                .authorize(authorizeReadState(testNode))
                .requireAll(
                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                requireThat("$current.api.username").isEqualTo().value(getSecondUser().getUsername())
                        )
                ));

        // when
        NodeResponse readNode = getSecondUser().readNode(ReadNodeRequest.builder()
                .id(testNode)
                .showState(ReadState.WITH_STATE)
                .authorization(authorization));

        // then
        assertThat(readNode).isNotNull();
        assertThat(readNode.getState()).isNotEmpty();

        // when
        ErrorResponse error = expectError(() -> getSecondUser().readNode(ReadNodeRequest.builder()
                .id(testNode)
                .showState(ReadState.WITH_STATE)));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldUseMultipleAuthorizationWhenLinkingNode() {
        // given
        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                        )
                                ))));

        NodeResponse authorizationForSecondUser = user.writeNode(AuthorizationNodeRequest.builder()
                .id(generateUniqueId())
                .authorize(authorizeLink(testNode, "#test"))
                .requireAll(
                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                requireThat("$current.api.username").isEqualTo().value(getSecondUser().getUsername())
                        )
                ));

        NodeResponse authorizationForThirdUser = getSecondUser().writeNode(AuthorizationNodeRequest.builder()
                .id(generateUniqueId())
                .authorize(authorizeLink(testNode, "#test"))
                .requireAll(
                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                requireThat("$current.api.username").isEqualTo().value(getThirdUser().getUsername())
                        )
                ));

        // when
        WriteResponse response = getThirdUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addRequest(LinkRequest.builder()
                        .sourceNode(0)
                        .targetNode(testNode)
                        .targetAnchor("#test")
                        .authorizations(authorizationForThirdUser, authorizationForSecondUser)));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> getThirdUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addRequest(LinkRequest.builder()
                        .sourceNode(0)
                        .targetNode(testNode)
                        .targetAnchor("#test")
                        .authorizations(authorizationForSecondUser, authorizationForThirdUser))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.AUTHORIZATION_PERMISSION_DENIED);
    }

    @Test
    public void shouldUseMultipleAuthorizationWhenReadingState() {
        // given
        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .permission(Permission.builder()
                                .allow(allowReadState())
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                        )
                                ))));

        NodeResponse authorizationForSecondUser = user.writeNode(AuthorizationNodeRequest.builder()
                .id(generateUniqueId())
                .authorize(authorizeReadState(testNode))
                .requireAll(
                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                requireThat("$current.api.username").isEqualTo().value(getSecondUser().getUsername())
                        )
                ));

        NodeResponse authorizationForThirdUser = getSecondUser().writeNode(AuthorizationNodeRequest.builder()
                .id(generateUniqueId())
                .authorize(authorizeReadState(testNode))
                .requireAll(
                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                requireThat("$current.api.username").isEqualTo().value(getThirdUser().getUsername())
                        )
                ));

        // when
        NodeResponse linkedNode = getThirdUser().readNode(ReadNodeRequest.builder()
                .id(testNode)
                .showState(ReadState.WITH_STATE)
                .authorizations(authorizationForThirdUser, authorizationForSecondUser));

        // then
        assertThat(linkedNode).isNotNull();

        // when
        ErrorResponse error = expectError(() -> getThirdUser().readNode(ReadNodeRequest.builder()
                .id(testNode)
                .showState(ReadState.WITH_STATE)
                .authorizations(authorizationForSecondUser, authorizationForThirdUser)));

        // then
        assertThat(error).hasErrorCode(ErrorCode.AUTHORIZATION_PERMISSION_DENIED);
    }

    @Test
    public void shouldCreateAuthorizationFromDocumentUpload() {
        // given
        String nodeId = generateUniqueId();
        String anchor = "#test";
        String userId = generateUniqueId();

        String authorizationText = String.format(
                "Authorization for:" +
                        "- linking to anchor %s of node %s.\n\n" +
                        "Authorization can be used when:\n" +
                        "- request is made by user %s using his API-key\n",
                anchor, nodeId, userId);

        AuthorizationNodeRequest authorizationObject = AuthorizationNodeRequest.builder()
                .authorize(authorizeLink(nodeId, anchor))
                .requireAll(
                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                requireThat("$current.api.username").isEqualTo().value(userId)
                        )
                )
                .build();

        byte[] authorizationPdf = PdfMother.createSignedPdf(authorizationText, authorizationObject);

        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.AUTHORIZATION)
                .file(authorizationPdf)
                .build();

        // when
        NodeResponse node = user.writeNode(request);

        // then
        assertThat(node.getType()).isEqualTo(NodeTypeId.simple(Types.AUTHORIZATION));
        assertThat(node.getState()).containsKeys("authorize", "require", "authorization_principals");

        Object signaturePrincipal = MapUtils.get(node.getState(), "authorization_principals", 1, "signature");
        assertThat(signaturePrincipal)
                .asInstanceOf(map(String.class, Object.class))
                .containsKeys(
                        "covers_whole_document",
                        "digest_algorithm",
                        "encryption_algorithm",
                        "filter",
                        "is_qualified",
                        "sign_date",
                        "signature_algorithm",
                        "signature_qualification",
                        "sub_filter",
                        "validation_failed",
                        "validation_passed"
                )
                .extractingByKey("certificate", map(String.class, Object.class))
                .containsKeys(
                        "issuer",
                        "issuer",
                        "self_issued",
                        "self_signed",
                        "serial_number",
                        "subject",
                        "subject",
                        "valid_at_sign_time",
                        "valid_now"
                );
    }

    @Test
    public void shouldUseDocumentAuthorizationWhenLinkingNode() {
        // given
        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.signature.certificate.subject.CN").isEqualTo().value("SLGL Integration Test")
                                        )
                                ))));

        String authorizationText = String.format(
                "Authorization for:" +
                        "- linking to anchor %s of node %s.\n\n" +
                        "Authorization can be used when:\n" +
                        "- request is made by user %s using his API-key\n",
                "#test", testNode.getId(), getSecondUser().getUsername());

        AuthorizationNodeRequest authorizationObject = AuthorizationNodeRequest.builder()
                .authorize(authorizeLink(testNode, "#test"))
                .requireAll(
                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                requireThat("$current.api.username").isEqualTo().value(getSecondUser().getUsername())
                        )
                )
                .build();

        byte[] authorizationPdf = PdfMother.createSignedPdf(authorizationText, authorizationObject);

        NodeResponse authorization = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .file(authorizationPdf)
                .type(Types.AUTHORIZATION));

        // when
        WriteResponse response = getSecondUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addRequest(LinkRequest.builder()
                        .sourceNode(0)
                        .targetNode(testNode)
                        .targetAnchor("#test")
                        .authorization(authorization)));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> getThirdUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addRequest(LinkRequest.builder()
                        .sourceNode(0)
                        .targetNode(testNode)
                        .targetAnchor("#test")
                        .authorization(authorization))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.AUTHORIZATION_PERMISSION_DENIED);

        // when
        error = expectError(() -> getSecondUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, testNode, "#test")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldReturnErrorForRequestWithNotExistingAuthorization() {
        // given
        String notExistingAuthorization = generateUniqueId();

        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .authorization(notExistingAuthorization)
                .build();

        // when
        ErrorResponse error = expectError(() -> user.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.AUTHORIZATION_DOESNT_EXIST);
    }

    @Test
    public void shouldReturnErrorForRequestWithNotNodeThatIsNotAuthorization() {
        // given
        NodeResponse notAuthorizationNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId()));

        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .authorization(notAuthorizationNode)
                .build();

        // when
        ErrorResponse error = expectError(() -> user.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.AUTHORIZATION_HAS_INVALID_TYPE);
    }

    @Test
    public void shouldReturnErrorForRequestWithLinkAuthorizationForDifferentAnchor() {
        // given
        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                        )
                                ))));

        NodeResponse authorization = user.writeNode(AuthorizationNodeRequest.builder()
                .id(generateUniqueId())
                .authorize(authorizeLink(testNode, "#other_anchor"))
                .alwaysAllowed());

        // when
        ErrorResponse error = expectError(() -> getSecondUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addRequest(LinkRequest.builder()
                        .sourceNode(0)
                        .targetNode(testNode)
                        .targetAnchor("#test")
                        .authorization(authorization))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.AUTHORIZATION_NOT_MATCHING_ACTION);
    }

    @Test
    public void shouldReturnErrorForRequestWithLinkAuthorizationForDifferentNode() {
        // given
        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#test")
                        .permission(Permission.builder()
                                .allow(allowLink("#test"))
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                        )
                                ))));

        NodeResponse otherNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId()));

        NodeResponse authorization = user.writeNode(AuthorizationNodeRequest.builder()
                .id(generateUniqueId())
                .authorize(authorizeLink(otherNode, "#test"))
                .alwaysAllowed());

        // when
        ErrorResponse error = expectError(() -> getSecondUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addRequest(LinkRequest.builder()
                        .sourceNode(0)
                        .targetNode(testNode)
                        .targetAnchor("#test")
                        .authorization(authorization))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.AUTHORIZATION_NOT_MATCHING_ACTION);
    }

    @Test
    public void shouldValidateAuthorizationObject() {
        // when
        var error = expectError(() -> user.writeNode(AuthorizationNodeRequest.builder()
                .id(generateUniqueId())
                .data("authorization_principals", ImmutableList.of("foo"))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].authorize", "not_empty")
                .hasFieldError("requests[0].authorization_principals", "null");

        // when
        error = expectError(() -> user.writeNode(AuthorizationNodeRequest.builder()
                .id(generateUniqueId())
                .data("authorize", ImmutableList.of(
                        Collections.emptyMap(),
                        new Authorize(AuthorizeAction.LINK_TO_ANCHOR, "invalid-url", "invalid-anchor")
                ))));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].authorize[0].action", "not_null")
                .hasFieldError("requests[0].authorize[0].node", "not_null")
                .hasFieldError("requests[0].authorize[1].node", "pattern")
                .hasFieldError("requests[0].authorize[1].anchor", "pattern");
    }
}
