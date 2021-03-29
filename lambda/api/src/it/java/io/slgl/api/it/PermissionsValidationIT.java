package io.slgl.api.it;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.NodeResponse;
import io.slgl.client.node.TypeNodeRequest;
import io.slgl.client.node.permission.Allow;
import io.slgl.client.node.permission.Permission;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;

public class PermissionsValidationIT extends AbstractApiTest {

    @Test
    public void shouldFailWhenCreatingTypeWithPermissionWithInvalidRequire() {
        // given
        TypeNodeRequest request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .data("permissions", ImmutableList.of(
                        ImmutableMap.of(
                                "require",
                                ImmutableMap.of(
                                        "some_path", ImmutableMap.of(
                                                "invalid_key", "example_value"
                                        )
                                )
                        )
                ))
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.UNABLE_TO_PARSE_UNRECOGNIZED_FIELD, "permissions[0].require.some_path.invalid_key");
    }

    @Test
    public void shouldFailWhenCreatingTypeWithPermissionWithBothAllowAndExtendsId() {
        // given
        TypeNodeRequest request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .permission(Permission.builder()
                        .id("example-id")
                        .allow(Allow.allowReadState())
                        .extendsId("example-extends-id"))
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    public void shouldFailWhenCreatingTypeWithPermissionThatExtendsPermissionWithNotExistingId() {
        // given
        NodeResponse baseType = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId())
                .permission(Permission.builder()
                        .id("allow-read-state-id")
                        .allow(Allow.allowReadState())
                        .alwaysAllowed()));

        TypeNodeRequest request = TypeNodeRequest.builder()
                .id(generateUniqueId())
                .extendsType(baseType)
                .permission(Permission.builder()
                        .extendsId("not-existing-id")
                        .alwaysAllowed())
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(request));

        // then
        assertThat(error).hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].permissions", "valid_type")
                .fieldErrorContainsMessage("requests[0].permissions", "not-existing-id");
    }
}
