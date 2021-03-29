package io.slgl.api.it;

import io.slgl.api.it.user.User;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.jsonlogic.*;
import io.slgl.client.node.*;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.PermissionMother.allowReadStateForEveryone;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.permission.Allow.*;
import static io.slgl.client.node.permission.Requirement.requireThat;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;

public class PermissionsIT extends AbstractApiTest {

    @Test
    public void shouldCheckPermissionsOnInlineType() {
        // given
        NodeResponse parent = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#child")
                        .permission(Permission.builder()
                                .allow(allowLink("#child"))
                                .requireAll(
                                        requireThat("$source_node.value").isEqualTo().value("valid")
                                ))));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .stateProperties("value")
                                .permission(allowReadStateForEveryone()))
                        .data("value", "valid"))
                .addLinkRequest(0, parent, "#child"));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .stateProperties("value")
                                .permission(allowReadStateForEveryone()))
                        .data("value", "not_valid"))
                .addLinkRequest(0, parent, "#child")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldThrowErrorOnEmptyAllowList() {
        // given
        var withPermissionWithoutAllow = List.of(
                TypeNodeRequest.builder()
                        .permission(Permission.builder()),
                NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .permission(Permission.builder())),
                NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .anchor("#anchor", TypeNodeRequest.builder()
                                        .permission(Permission.builder())))
        );

        // then
        for (var request : withPermissionWithoutAllow) {
            softly.assertApiException(() -> ledger.writeNode(request))
                    .hasValidationError(Pattern.compile(".*\\.allow"), "not_empty");
        }
    }

    @Test
    public void shouldCheckPermissionsOnStandaloneType() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#child")
                .permission(Permission.builder()
                        .allow(allowLink("#child"))
                        .requireAll(
                                requireThat("$source_node.value").isEqualTo().value("valid")
                        )));

        NodeResponse parent = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(type));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .stateProperties("value")
                                .permission(allowReadStateForEveryone()))
                        .data("value", "valid"))
                .addLinkRequest(0, parent, "#child"));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(TypeNodeRequest.builder()
                                .stateProperties("value")
                                .permission(allowReadStateForEveryone()))
                        .data("value", "not_valid"))
                .addLinkRequest(0, parent, "#child")));


        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldAllowWithPermissionWithNoRequireAndNoRequireLogic() {
        // given
        NodeResponse parent = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#child1")
                        .anchor("#child2")
                        .permissions(Permission.builder()
                                .allow(allowLink("#child1"))))
        );

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, parent, "#child1"));

        // then
        assertThat(response).isNotNull();
    }

    @Test
    public void shouldWorkWithMultipleRequirementsOnTheSamePath() {
        // given
        NodeResponse parent = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#child")
                        .permission(Permission.builder()
                                .allow(allowLink("#child"))
                                .requireAll(
                                        requireThat("$source_node.value").contains().value("valid"),
                                        requireThat("$source_node.value").doesNotContain().value("invalid")
                                ))));

        NodeResponse childType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("value")
                .permission(allowReadStateForEveryone()));

        // then
        softly.assertApiException(() -> {
            getSecondUser().write(WriteRequest.builder()
                    .addRequest(NodeRequest.builder()
                            .type(childType)
                            .data("value", singletonList("invalid")))
                    .addLinkRequest(0, parent, "#child"));
        })
                .hasErrorCode(ErrorCode.PERMISSION_DENIED);

        softly.assertApiException(() -> {
            getSecondUser().write(WriteRequest.builder()
                    .addRequest(NodeRequest.builder()
                            .type(childType)
                            .data("value", asList("valid", "invalid")))
                    .addLinkRequest(0, parent, "#child"));
        })
                .hasErrorCode(ErrorCode.PERMISSION_DENIED);

        softly.assertThatCode(() -> {
            getSecondUser().write(WriteRequest.builder()
                    .addRequest(NodeRequest.builder()
                            .type(childType)
                            .data("value", asList("valid", "ignored")))
                    .addLinkRequest(0, parent, "#child"));
        })
                .doesNotThrowAnyException();
    }

    @Test
    public void shouldCheckBothBaseAndExtendedPermissionsWhenLinkingEntry() {
        // given
        NodeResponse baseType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#child")
                .permission(Permission.builder()
                        .id("base-permission")
                        .allow(allowLink("#child"))
                        .requireAll(
                                requireThat("$source_node.base_value").isEqualTo().value("valid")
                        )));

        NodeResponse extendedType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .extendsType(baseType)
                .permission(Permission.builder()
                        .allow(allowAll())
                        .extendsId("base-permission")
                        .requireAll(
                                requireThat("$source_node.extended_value").isEqualTo().value("valid")
                        )));

        NodeResponse parent = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(extendedType));

        NodeResponse childType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("base_value", "extended_value")
                .permission(allowReadStateForEveryone()));

        // when
        WriteResponse response = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(childType)
                        .data("base_value", "valid")
                        .data("extended_value", "valid"))
                .addLinkRequest(0, parent, "#child"));

        // then
        assertThat(response).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(childType)
                        .data("base_value", "valid")
                        .data("extended_value", "not_valid"))
                .addLinkRequest(0, parent, "#child")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // when
        ErrorResponse otherError = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(childType)
                        .data("base_value", "not_valid")
                        .data("extended_value", "valid"))
                .addLinkRequest(0, parent, "#child")));

        // then
        assertThat(otherError).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldFailWithImpossibleCustomPermissionLogic() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#anchor")
                        .permission(Permission.builder()
                                .allow(allowLink("#anchor"))
                                .requireLogic(new Equal(0, 1)))));

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#anchor")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldSupportCustomPermissionsLogicThatChecksIfOtherAnchorExist() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#first")
                        .anchor("#second")
                        .permission(Permission.builder()
                                .allow(allowLink("#first"))
                                .requireLogic(true))
                        .permission(Permission.builder()
                                .allow(allowLink("#second"))
                                .requireLogic(new GreaterThan(new Var("$target_node.#first.$length"), 0)))));

        WriteRequest.Builder linkingRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#second");

        // when
        ErrorResponse error = expectError(() -> ledger.write(linkingRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#first"));

        // when
        WriteResponse linkingResponse = ledger.write(linkingRequest);

        // then
        assertThat(linkingResponse).isNotNull();
    }

    @Test
    public void shouldGracefullyHandleIndexOutOfBoundsWhenAccessingAnchorsInCustomPermissionLogic() {
        // given
        NodeResponse node = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#payment")
                        .anchor("#action")
                        .permission(Permission.builder()
                                .allow(allowLink("#action"))
                                .requireLogic(new GreaterThan(
                                        new Var("$target_node.#payment.$newest.amount"),
                                        10
                                )))));

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, node, "#action")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    /*
    @Test
    public void shouldSupportCustomPermissionsLogicThatChecksParentNodes() {
        // given
        NodeResponse rootEntry = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId("entry"))
                .type(TypeNodeRequest.builder()
                        .stateProperties("value")
                        .anchor("#child", TypeNodeRequest.builder()
                                .stateProperties("value")
                                .anchor("#child")
                                .permission(Permission.builder()
                                        .allow(allowLink("#child"))
                                        .requireLogic(new And(
                                                new Equal(new Var("$parent.value"), "child"),
                                                new Equal(new Var("$parent.$parent.value"), "root")
                                        )))
                                .permission(Permission.builder()
                                        .allow(allowReadState())
                                        .requireLogic(true)))
                        .permission(Permission.builder()
                                .allow(allowLink("#child"))
                                .requireLogic(true))
                        .permission(Permission.builder()
                                .allow(allowReadState())
                                .requireLogic(true)))
                .data("value", "root"));

        NodeResponse childEntry = ledger.writeNode(NodeRequest.builder().link(rootEntry, "#child")
                .data("value", "child"));

        // when
        NodeResponse secondChildEntry = ledger.writeNode(NodeRequest.builder().link(childEntry, "#child"));

        // then
        assertThat(secondChildEntry).isNotNull();
    }
    */

    @Test
    public void shouldSupportCustomPermissionLogicThatChecksValueFromLatestAnchor() {
        // given
        NodeResponse tokenBalanceType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("amount")
                .permission(Permission.builder()
                        .allow(allowReadState())
                        .alwaysAllowed()));

        NodeResponse userType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .anchor("#token_balance", tokenBalanceType.getId())
                .anchor("#action")
                .permission(Permission.builder()
                        .allow(allowLink("#token_balance"))
                        .requireLogic(true))
                .permission(Permission.builder()
                        .allow(allowLink("#action"))
                        .requireLogic(new GreaterThan(
                                new Var("$target_node.#token_balance.$newest.amount"),
                                10
                        ))));

        NodeResponse userNode = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(userType));

        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(tokenBalanceType)
                        .data("amount", 3))
                .addLinkRequest(0, userNode, "#token_balance"));

        WriteRequest actionRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, userNode, "#action")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(actionRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(tokenBalanceType)
                        .data("amount", 13))
                .addLinkRequest(0, userNode, "#token_balance"));

        // when
        WriteResponse actionNode = ledger.write(actionRequest);

        // then
        assertThat(actionNode).isNotNull();

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(tokenBalanceType)
                        .data("amount", 8))
                .addLinkRequest(0, userNode, "#token_balance"));

        // when
        error = expectError(() -> ledger.write(actionRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldSupportPermissionsWithPaths() {
        // given
        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .stateProperties("string_value", "int_value", "small_value", "big_value")
                .permission(allowReadStateForEveryone()));

        NodeResponse entity = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#child")
                        .permissions(Permission.builder()
                                .allow(allowLink("#child"))
                                .requireAll(
                                        requireThat("$source_node.string_value").isEqualTo().value("example"),
                                        requireThat("$source_node.int_value").isEqualTo().value(123),
                                        requireThat("$source_node.small_value").isLessThan().ref("$source_node.big_value")
                                ))));

        // when
        WriteResponse linkResponse = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(type)
                        .data("string_value", "example")
                        .data("int_value", 123)
                        .data("small_value", 1)
                        .data("big_value", 999))
                .addLinkRequest(0, entity, "#child"));

        // then
        assertThat(linkResponse).isNotNull();

        // when
        ErrorResponse error = expectError(() -> ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(type)
                        .data("string_value", "different")
                        .data("int_value", 7)
                        .data("small_value", 99)
                        .data("big_value", 11))
                .addLinkRequest(0, entity, "#child")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldSupportPermissionThatSumsAmountFromAllAnchorsUsingCustomLogic() {
        Permission.Builder permission = Permission.builder()
                .requireLogic(new GreaterThan(
                        new Reduce(
                                new Var("$target_node.#payment"),
                                new Add(new Var("$accumulator"), new Var("$current.amount")),
                                0
                        ),
                        100
                ));

        shouldSupportPermissionThatSumsAmountFromAllAnchors(permission);
    }

    @Test
    public void shouldSupportPermissionThatSumsAmountFromAllAnchorsUsingPathExpressions() {
        Permission.Builder permission = Permission.builder()
                .requireAll(
                        requireThat("$target_node.#payment.amount").sum().isGreaterThanOrEqualTo().value(100)
                );

        shouldSupportPermissionThatSumsAmountFromAllAnchors(permission);
    }

    private void shouldSupportPermissionThatSumsAmountFromAllAnchors(Permission.Builder permissionNodeRequest) {
        // given
        NodeResponse paymentType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("amount")
                .permission(Permission.builder()
                        .allow(allowReadState())
                        .alwaysAllowed()));

        NodeResponse contractNode = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#payment", paymentType.getId())
                        .anchor("#finalize")
                        .permission(Permission.builder()
                                .allow(allowLink("#payment"))
                                .requireLogic(true))
                        .permission(permissionNodeRequest
                                .allow(allowLink("#finalize")))));

        WriteRequest finalizeRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, contractNode, "#finalize")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(finalizeRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(paymentType)
                        .data("amount", 59))
                .addLinkRequest(0, contractNode, "#payment"));

        // when
        error = expectError(() -> ledger.write(finalizeRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(paymentType)
                        .data("amount", 59))
                .addLinkRequest(0, contractNode, "#payment"));

        // when
        WriteResponse finalizeResponse = ledger.write(finalizeRequest);

        // then
        assertThat(finalizeResponse).isNotNull();
    }

    @Test
    public void shouldSupportPermissionThatSumsAmountFromBothInlineAndLinkedAnchors() {
        // given
        NodeResponse paymentType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .stateProperties("amount")
                .permission(Permission.builder()
                        .allow(allowReadState())
                        .alwaysAllowed()));

        NodeResponse contractNode = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#payment", paymentType)
                        .anchor("#finalize")
                        .permission(Permission.builder()
                                .allow(allowLink("#payment"))
                                .alwaysAllowed())
                        .permission(Permission.builder()
                                .allow(allowLink("#finalize"))
                                .requireAll(
                                        requireThat("$target_node.#payment.amount").sum().isGreaterThanOrEqualTo().value(100)
                                )))
                .linkNode("#payment", NodeRequest.builder()
                        .data("amount", 40)));

        WriteRequest finalizeRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, contractNode, "#finalize")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(finalizeRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(paymentType)
                        .data("amount", 40))
                .addLinkRequest(0, contractNode, "#payment"));

        // when
        error = expectError(() -> ledger.write(finalizeRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(paymentType)
                        .data("amount", 40))
                .addLinkRequest(0, contractNode, "#payment"));

        // when
        WriteResponse finalizeResponse = ledger.write(finalizeRequest);

        // then
        assertThat(finalizeResponse).isNotNull();
    }

    @Test
    public void shouldValidateStateAccessWhenCheckingPermission() {
        // given
        NodeResponse entry = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .stateProperties("value")
                        .anchor("#child")
                        .permission(Permission.builder()
                                .allow(allowLink("#child"))
                                .requireAll(
                                        requireThat("$target_node.value").isEqualTo().value("foo")
                                ))
                        .permission(Permission.builder()
                                .allow(allowReadState())
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                        )
                                )))
                .data("value", "foo"));

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, entry, "#child")
                .build();

        // when
        WriteResponse linkResponse = user.write(linkRequest);

        // then
        assertThat(linkResponse).isNotNull();

        // when
        ErrorResponse error = expectError(() -> getSecondUser().write(linkRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldAllowToEvaluatePermissionAsOtherUser() {
        // given
        NodeResponse entry = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .stateProperties("value")
                        .anchor("#child")
                        .permission(Permission.builder()
                                .allow(allowReadState())
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                        )
                                ))
                        .permission(Permission.builder()
                                .allow(allowLink("#child"))
                                .requireAll(
                                        requireThat("$target_node.value").isEqualTo().value("foo")
                                )
                                .evaluateStateAccessAsUser(user.getUsername())))
                .data("value", "foo"));

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, entry, "#child")
                .build();

        WriteResponse linkResponse = user.write(linkRequest);
        assertThat(linkResponse).isNotNull();

        // when
        WriteResponse linkAsOtherUserResponse = getSecondUser().write(linkRequest);

        // then
        assertThat(linkAsOtherUserResponse).isNotNull();
    }

    @Test
    public void shouldAllowToCompareNodeCreatedDate() {
        // given
        OffsetDateTime dateInPast = OffsetDateTime.now().minusDays(5);
        OffsetDateTime dateInFuture = OffsetDateTime.now().plusDays(5);

        NodeResponse entry = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#date_in_past")
                        .anchor("#date_in_future")
                        .permission(Permission.builder()
                                .allow(allowLink("#date_in_past"))
                                .requireAll(
                                        requireThat("$source_node.created").isAfter().value(dateInPast)
                                ))
                        .permission(Permission.builder()
                                .allow(allowLink("#date_in_future"))
                                .requireAll(
                                        requireThat("$source_node.created").isAfter().value(dateInFuture)
                                )))
        );

        // when
        WriteResponse linkResponse = user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, entry, "#date_in_past"));

        // then
        assertThat(linkResponse).isNotNull();

        // when
        ErrorResponse error = expectError(() -> user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, entry, "#date_in_future")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldAllowToCompareLinkCreatedDate() {
        // given
        OffsetDateTime dateInPast = OffsetDateTime.now().minusDays(5);
        OffsetDateTime dateInFuture = OffsetDateTime.now().plusDays(5);

        NodeResponse entry = user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#date_in_past")
                        .anchor("#date_in_future")
                        .permission(Permission.builder()
                                .allow(allowLink("#date_in_past"))
                                .requireAll(
                                        requireThat("$created").isAfter().value(dateInPast)
                                ))
                        .permission(Permission.builder()
                                .allow(allowLink("#date_in_future"))
                                .requireAll(
                                        requireThat("$created").isAfter().value(dateInFuture)
                                )))
        );

        // when
        WriteResponse linkResponse = user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, entry, "#date_in_past"));

        // then
        assertThat(linkResponse).isNotNull();

        // when
        ErrorResponse error = expectError(() -> user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, entry, "#date_in_future")));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldSupportPermissionThatChecksIfNodeWithGivenIdExists() {
        // given
        String testId = generateUniqueId();

        NodeResponse entry = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#child")
                        .permission(Permission.builder()
                                .allow(allowLink("#child"))
                                .requireAll(
                                        requireThat("@('" + testId + "')").isNotEqualTo().value(null)
                                ))));

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, entry, "#child")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(linkRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.writeNode(NodeRequest.builder().id(testId));

        // when
        WriteResponse linkResponse = ledger.write(linkRequest);

        // then
        assertThat(linkResponse).isNotNull();
    }

    @Test
    public void shouldSupportPermissionThatChecksAnchorsOfNodeWithGivenId() {
        // given
        NodeResponse testEntry = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#allow")
                        .permission(Permission.builder()
                                .allow(allowLink("#allow"))
                                .alwaysAllowed())));

        NodeResponse entry = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#child")
                        .permission(Permission.builder()
                                .allow(allowLink("#child"))
                                .requireAll(
                                        requireThat("@('" + testEntry.getId() + "').#allow.$length").isEqualTo().value(1)
                                ))));

        WriteRequest linkRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, entry, "#child")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(linkRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, testEntry, "#allow"));

        // when
        WriteResponse linkResponse = ledger.write(linkRequest);

        // then
        assertThat(linkResponse).isNotNull();
    }

    @Test
    public void shouldSupportPermissionThatChecksIfAnyElementFromListMeetsGivenRequirements() {
        // given
        NodeResponse dataType = ledger.writeNode(TypeNodeRequest.builder()
                .stateProperties("is_valid", "value")
                .permission(allowReadStateForEveryone()));

        NodeResponse entry = ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#data", TypeNodeRequest.builder()
                                .stateProperties("is_valid", "value")
                                .permission(Permission.builder()
                                        .allow(allowReadState())
                                        .alwaysAllowed()))
                        .anchor("#finalize")
                        .permission(Permission.builder()
                                .allow(allowLink("#data"))
                                .requireLogic(true))
                        .permission(Permission.builder()
                                .allow(allowLink("#finalize"))
                                .requireAll(
                                        requireThat("$target_node.#data").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.is_valid").isEqualTo().value(true),
                                                requireThat("$current.value").isGreaterThanOrEqualTo().value(100)
                                        )
                                )
                        )));

        WriteRequest finalizeRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, entry, "#finalize")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.write(finalizeRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(dataType)
                        .data("is_valid", false)
                        .data("value", 1))
                .addLinkRequest(0, entry, "#data"));

        // when
        error = expectError(() -> ledger.write(finalizeRequest));

        // then
        assertThat(error).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        // given
        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(dataType)
                        .data("is_valid", true)
                        .data("value", 120))
                .addLinkRequest(0, entry, "#data"));

        // when
        WriteResponse finalizeResponse = ledger.write(finalizeRequest);

        // then
        assertThat(finalizeResponse).isNotNull();
    }


    @Test
    public void shouldSupportPermissionThatChecksIfAnyElementFromListMeetsGivenRequirementsAndOneOfItemsHasNoReadPermission() {
        // given
        var rootId = generateUniqueId();
        var onlyAllowedDataIdToBeRead = generateUniqueId();

        NodeResponse dataType = ledger.writeNode(TypeNodeRequest.builder()
                .stateProperties("is_valid")
                .permission(Permission.builder()
                        .allow(allowReadState())
                        .requireAll(requireThat("$node.@id").isEqualTo().value(onlyAllowedDataIdToBeRead))
                ));

        NodeResponse entry = ledger.writeNode(NodeRequest.builder()
                .id(rootId)
                .type(TypeNodeRequest.builder()
                        .anchor("#data", dataType)
                        .permission(Permission.builder()
                                .allow(allowLink("#data")))

                        .anchor("#finalize")
                        .permission(Permission.builder()
                                .allow(allowLink("#finalize"))
                                .requireAll(
                                        requireThat("$target_node.#data").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.is_valid").isEqualTo().value(true)
                                        )
                                )
                        )));

        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .id(generateUniqueId())
                        .type(dataType)
                        .data("is_valid", true))
                .addRequest(NodeRequest.builder()
                        .id(onlyAllowedDataIdToBeRead)
                        .type(dataType)
                        .data("is_valid", true))
                .addRequest(NodeRequest.builder()
                        .id(generateUniqueId())
                        .type(dataType)
                        .data("is_valid", true))
                .addLinkRequest(0, entry, "#data")
                .addLinkRequest(1, entry, "#data")
                .addLinkRequest(2, entry, "#data"));

        // when
        WriteRequest finalizeRequest = WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, entry, "#finalize")
                .build();
        // then
        assertThatCode(() -> ledger.write(finalizeRequest))
                .doesNotThrowAnyException();
    }

    @Test
    public void shouldHandleSmartContractForVotingPoll() {
        // given
        NodeResponse participantType = ledger.writeNode(TypeNodeRequest.builder()
                .anchor("#delete")
                .stateProperties("username")
                .permission(allowReadStateForEveryone())
                .permission(Permission.builder()
                        .allow(allowLink("#delete"))
                        .requireAll(
                                requireThat("$principals").atLeastOne().meetsAllRequirements(
                                        requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                )
                        )));

        NodeResponse voteType = ledger.writeNode(TypeNodeRequest.builder()
                .stateProperties("username")
                .permission(allowReadStateForEveryone()));

        NodeRequest poolRequest = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#participant", participantType)
                        .anchor("#vote", voteType)
                        .permission(Permission.builder()
                                .allow(allowLink("#participant"))
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().value(user.getUsername())
                                        )
                                ))
                        .permission(Permission.builder()
                                .allow(allowLink("#vote"))
                                .requireAll(
                                        requireThat("$principals").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.api.username").isEqualTo().ref("$source_node.username")
                                        ),
                                        requireThat("$target_node.#vote.username").doesNotContain().ref("$source_node.username"),
                                        requireThat("$target_node.#participant").atLeastOne().meetsAllRequirements(
                                                requireThat("$current.username").isEqualTo().ref("$source_node.username"),
                                                requireThat("$current.#delete.$length").isEqualTo().value(0)
                                        )
                                )))
                .build();

        // when
        NodeResponse pollEntry = ledger.writeNode(poolRequest);

        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(participantType)
                        .data("username", getSecondUser().getUsername()))
                .addLinkRequest(0, pollEntry, "#participant"));

        NodeResponse participant = ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(participantType)
                        .data("username", getThirdUser().getUsername()))
                .addLinkRequest(0, pollEntry, "#participant"))
                .getNodes().get(0);

        ledger.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder())
                .addLinkRequest(0, participant, "#delete"));

        // then
        Function<User, NodeResponse> vote = user -> user.write(WriteRequest.builder()
                .addRequest(NodeRequest.builder()
                        .type(voteType)
                        .data("username", user.getUsername()))
                .addLinkRequest(0, pollEntry, "#vote"))
                .getNodes().get(0);

        assertThat(expectError(() -> vote.apply(user))).hasErrorCode(ErrorCode.PERMISSION_DENIED);
        assertThat(expectError(() -> vote.apply(getThirdUser()))).hasErrorCode(ErrorCode.PERMISSION_DENIED);

        assertThat(vote.apply(getSecondUser())).isNotNull();
        assertThat(expectError(() -> vote.apply(getSecondUser()))).hasErrorCode(ErrorCode.PERMISSION_DENIED);
    }

    @Test
    public void shouldValidatePermissionObject() {
        // given
        TypeNodeRequest request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .permission(Permission.builder()
                        .evaluateStateAccessAsUser("other-user"))
                .build();

        // when
        var error = expectError(() -> user.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].permissions[0].evaluate_state_access_as_user", "must_be_current_user");
    }
}
