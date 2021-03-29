package io.slgl.api.it;

import io.slgl.api.it.junit.extension.AuditSQS;
import io.slgl.api.it.junit.extension.AuditSQSExtension;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.Types;
import io.slgl.client.audit.PermissionEvaluationType;
import io.slgl.client.audit.RequestType;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.jsonlogic.*;
import io.slgl.client.node.*;
import io.slgl.client.node.authorization.AuthorizationNodeRequest;
import io.slgl.client.node.permission.Permission;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static io.slgl.api.it.assertj.SlglAssertions.assertApiException;
import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.assertj.SlglSoftAssertions.softly;
import static io.slgl.api.it.data.PermissionMother.allowReadStateForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.audit.RequestType.USE_AUTHORIZATION;
import static io.slgl.client.node.AuditorNodeRequest.AuditPolicy.*;
import static io.slgl.client.node.authorization.Authorize.authorizeLink;
import static io.slgl.client.node.permission.Allow.allowLink;
import static io.slgl.client.node.permission.Allow.allowReadState;
import static io.slgl.client.node.permission.Requirement.requireThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.waitAtMost;

@ExtendWith(AuditSQSExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class AuditorNotifyIT extends AbstractApiTest {

    @Test
    public void shouldIncludeNullResultsInEvaluatedVariables(AuditSQS auditSqs) {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#auditors", 1, Types.AUDITOR)
                        .anchor("#anchor")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .requireAll(
                                        requireThat("$node.not_existing_key").isEqualTo().value("does not matter")
                                )))
                .linkAuditor(user.auditor(FAILED))
                .build();

        NodeResponse node = ledger.writeNode(request);

        // when
        assertApiException(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor")));

        // then
        var auditData = waitAtMost(10, TimeUnit.SECONDS)
                .until(() -> auditSqs.getByNodeAndAnchor(node, "#anchor"),
                        CollectionUtils::isNotEmpty);

        assertThat(auditData).hasOnlyOneElementSatisfying(audit -> assertThat(audit)
                .hasRequestType(RequestType.LINK_NODE)
                .hasEvaluatedVariable("$node.not_existing_key", null)
                .isFailed()
        );
    }

    @Test
    public void shouldIncludeOnlyAccessedDataInEvaluationResultContext(AuditSQS auditSqs) {
        // given
        var nodeData = Map.of("data", Map.of(
                "prop1", 1,
                "prop2", 2,
                "prop3", 3,
                "prop4", Map.of(
                        "prop41", 41,
                        "prop42", 42
                )
        ));

        var requireLogic = new And(
                new Equal(new VarMap("prop1", new Var("$target_node.data")), 1),
                new Equal(new Var("$target_node.data.prop2"), 2),
                new Equal(new Var("$target_node.data.prop4.prop41"), 41)
        );

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .stateProperties("data")
                        .anchor("#anchor")
                        .permissions(
                                Permission.builder().allow(allowReadState()).alwaysAllowed(),
                                Permission.builder().allow(allowLink("#anchor")).requireLogic(requireLogic)
                        ))
                .data(nodeData)
                .linkAuditor(user.auditor(SUCCEED)));

        // when
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor"));

        // then
        var auditData = waitAtMost(10, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(() -> auditSqs.getSingleByNodeAndAnchor(node, "#anchor"), Objects::nonNull);

        assertThat(auditData.getPermissionAudit().getEvaluatedPermissions().stream()
                .filter(permissionEvaluation -> permissionEvaluation.getEvaluationType() == PermissionEvaluationType.LINK_NODE)
                .flatMap(permissionEvaluation -> permissionEvaluation.getEvaluationResults().stream())
        ).hasOnlyOneElementSatisfying(permissionEvaluationResult -> {
            assertThat(permissionEvaluationResult)
                    .hasPermissionContextValueAtPath(1, "$target_node", "data", "prop1")
                    .hasPermissionContextValueAtPath(2, "$target_node", "data", "prop2")
                    .doesNotHavePermissionContextValueAtPath("$target_node", "data", "prop3")
                    .hasPermissionContextValueAtPath(41, "$target_node", "data", "prop4", "prop41")
                    .doesNotHavePermissionContextValueAtPath("$target_node", "data", "prop4", "prop42")
                    .isSuccess();
        });
    }

    @Test
    public void shouldProperlyLogEvaluatedNestedJsonLogicVariables(AuditSQS auditSqs) {
        // given
        var nodeData = Map.of("data", Map.of(
                "string", "stringValue",
                "number", 1,
                "boolean", true,
                "object", Map.of("nestedString", "nestedStringValue")
        ));

        var requireLogic = new And(
                new NotEqual("ignored", new VarMap("object.nestedString", new Var("$target_node.data"))),
                new NotEqual("ignored", new VarMap("value_on_null", new Var("$target_node.data.not_defined"))),
                new NotEqual("ignored", new VarMap("value_on_number", new Var("$target_node.data.number"))),
                new NotEqual("ignored", new VarMap("value_on_boolean", new Var("$target_node.data.boolean"))),
                new NotEqual("ignored", new VarMap("value_on_string", new Var("$target_node.data.string")))
        );

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .stateProperties("data")
                        .anchor("#anchor")
                        .permissions(
                                Permission.builder().allow(allowReadState()).alwaysAllowed(),
                                Permission.builder().allow(allowLink("#anchor")).requireLogic(requireLogic)
                        ))
                .data(nodeData)
                .linkAuditor(user.auditor(SUCCEED)));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor"));

        // then
        var auditData = waitAtMost(10, TimeUnit.SECONDS)
                .until(() -> auditSqs.getByNodeAndAnchor(node, "#anchor"),
                        CollectionUtils::isNotEmpty);

        assertThat(auditData).anySatisfy(audit -> assertThat(audit)
                .hasNodeAndAnchorMatchingLink(response.getLinks().get(0))
                .hasRequestType(RequestType.LINK_NODE)
                .hasEvaluatedVariable("object.nestedString", "nestedStringValue")
                .hasEvaluatedVariable("value_on_null", null)
                .hasEvaluatedVariable("value_on_number", null)
                .hasEvaluatedVariable("value_on_boolean", null)
                .hasEvaluatedVariable("value_on_string", null)
                .isSuccess()
        );
    }

    @Test
    void shouldNotifyOnFailure(AuditSQS auditSqs) {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#auditors", 1, Types.AUDITOR)
                        .anchor("#anchor")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .requireLogic(false)))
                .linkAuditor(user.auditor(FAILED)));

        // when
        assertApiException(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor")));

        // then
        waitAtMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(auditSqs.getByNodeAndAnchor(node, "#anchor"))
                        .anySatisfy(audit -> assertThat(audit)
                                .hasNode(node)
                                .hasAnchor("#anchor")
                                .hasRequestType(RequestType.LINK_NODE)
                                .isFailed()
                        )
        );
    }

    @Test
    void shouldNotifyOnMaxAnchorExceededFailure(AuditSQS auditSqs) {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor", 1)
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .alwaysAllowed()))
                .linkAuditor(user.auditor(FAILED)));

        // when
        assertThatCode(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor")))
                .doesNotThrowAnyException();
        assertApiException(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor")));

        // then
        waitAtMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(auditSqs.getByNodeAndAnchor(node, "#anchor"))
                        .hasOnlyOneElementSatisfying(audit -> assertThat(audit)
                                .hasRequestType(RequestType.LINK_NODE)
                                .isFailed()
                                .hasEvaluationLogEntryWithCode("anchor_max_size_exceeded")
                        )
        );
    }

    @Test
    void shouldFailOnNonURLQueueAddress() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .requireLogic(false)))
                .linkAuditor(user.auditor(FAILED).awsSqs("not an url"))
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void shouldReadStateAndLinkEvenIfAuditorFails() {
        // given
        var node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permissions(Permission.builder().allow(allowLink("#anchor"), allowReadState())))
                .linkAuditor(user.auditor(ALL).awsSqs("https://example.com/not-an-real-sqs")));

        // when
        softly.assertThatCode(() -> ledger.readNode(node.getId(), ReadState.WITH_STATE))
                .as("read state")
                .doesNotThrowAnyException();
        softly.assertThatCode(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor")))
                .as("link to #anchor")
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotifyOnSuccess(AuditSQS auditSqs) {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#auditors", 1, Types.AUDITOR)
                        .anchor("#anchor")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .alwaysAllowed()))
                .linkAuditor(user.auditor(SUCCEED))
                .build();

        NodeResponse node = ledger.writeNode(request);

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor"));

        // then
        waitAtMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(auditSqs.getByNodeAndAnchor(node, "#anchor"))
                        .anySatisfy(audit -> assertThat(audit)
                                .hasNodeAndAnchorMatchingLink(response.getLinks().get(0))
                                .hasRequestType(RequestType.LINK_NODE)
                                .isSuccess()
                        )
        );
    }

    @Test
    void shouldIncludeEvaluatedPathInAudit(AuditSQS auditSqs) {
        // given
        NodeResponse anchorType = ledger.writeNode(TypeNodeRequest.builder()
                .stateProperties("child_key")
                .permission(allowReadStateForEveryone()));

        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .data("key", "value")
                .type(TypeNodeRequest.builder()
                        .stateProperties("key")
                        .anchor("#auditors", 1, Types.AUDITOR)
                        .anchor("#anchor", anchorType)
                        .permission(allowReadStateForEveryone())
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .requireAll(
                                        requireThat("$source_node.child_key").isEqualTo().ref("$target_node.key"),
                                        requireThat("$target_node.#auditors.$length").isEqualTo().value(1)
                                )))
                .linkAuditor(user.auditor(ALL)));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(anchorType)
                        .data("child_key", "value"))
                .addLinkRequest(0, node, "#anchor"));

        // then
        waitAtMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(auditSqs.getByNodeAndAnchor(node, "#anchor"))
                    .anySatisfy(audit ->
                            softly(softly -> softly.assertThat(audit)
                                    .hasNodeAndAnchorMatchingLink(response.getLinks().get(0))
                                    .hasRequestType(RequestType.LINK_NODE)
                                    .hasEvaluatedVariable("$source_node.child_key", "value")
                                    .hasEvaluatedVariable("$target_node.#auditors.$length", 1)
                                    .hasEvaluatedVariable("$target_node.key", "value")
                                    .isSuccess()
                            )
                    );
        });
    }

    @Test
    public void shouldNotifyForAuthorization(AuditSQS auditSqs) {
        // given
        NodeResponse testType = user.writeNode(TypeNodeRequest.builder()
                .stateProperties("value")
                .permission(allowReadStateForEveryone()));

        NodeResponse testNode = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#test", testType)
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
                .requireAll(requireThat("$source_node.value").isEqualTo().value("allow"))
                .linkAuditor(user.auditor(ALL)));

        // when
        getSecondUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(testType)
                        .data("value", "allow"))
                .addRequest(LinkRequest.builder()
                        .sourceNode(0)
                        .targetNode(testNode)
                        .targetAnchor("#test")
                        .authorization(authorization)));

        expectError(() -> getSecondUser().write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(testType)
                        .data("value", "deny"))
                .addRequest(LinkRequest.builder()
                        .sourceNode(0)
                        .targetNode(testNode)
                        .targetAnchor("#test")
                        .authorization(authorization))));

        // then
        waitAtMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(auditSqs.getByNode(authorization))
                    .hasSizeGreaterThanOrEqualTo(2)
                    .anySatisfy(message -> {
                        assertThat(message)
                                .hasNode(authorization)
                                .hasRequestType(USE_AUTHORIZATION)
                                .isSuccess();
                    })
                    .anySatisfy(message -> {
                        assertThat(message)
                                .hasNode(authorization)
                                .hasRequestType(USE_AUTHORIZATION)
                                .isFailed();
                    });
        });
    }
}
