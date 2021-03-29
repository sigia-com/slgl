package io.slgl.api.it;

import com.google.common.collect.ImmutableMap;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.Types;
import io.slgl.client.node.NodeRequest;
import io.slgl.client.node.NodeResponse;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;

public class ValueTypeIT extends AbstractApiTest {

    @Test
    public void shouldCreateTextValueNode() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.TEXT)
                .data("value", "example-text")
                .build();

        // when
        NodeResponse node = user.writeNode(request);

        // then
        assertThat(node).isNotNull();
        assertThat(node.getState()).containsEntry("value", "example-text");
    }

    @Test
    public void shouldValidateTextValueNode() {
        // when
        var error = expectError(() -> user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.TEXT)
                .data("value", ImmutableMap.of("foo", "bar"))));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.UNABLE_TO_PARSE_FIELD, "value");

        // when
        error = expectError(() -> user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.TEXT)));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].value", "not_null");
    }

    @Test
    public void shouldCreateNumberValueNode() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.NUMBER)
                .data("value", 42)
                .build();

        // when
        NodeResponse node = user.writeNode(request);

        // then
        assertThat(node).isNotNull();
        assertThat(node.getState()).containsEntry("value", 42);
    }

    @Test
    public void shouldValidateNumberValueNode() {
        // when
        var error = expectError(() -> user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.NUMBER)
                .data("value", "forty two")));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.UNABLE_TO_PARSE_FIELD, "value");

        // when
        error = expectError(() -> user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.NUMBER)));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].value", "not_null");
    }

    @Test
    public void shouldCreateBooleanValueNode() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.BOOLEAN)
                .data("value", true)
                .build();

        // when
        NodeResponse node = user.writeNode(request);

        // then
        assertThat(node).isNotNull();
        assertThat(node.getState()).containsEntry("value", true);
    }

    @Test
    public void shouldValidateBooleanValueNode() {
        // when
        var error = expectError(() -> user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.BOOLEAN)
                .data("value", "maybe")));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.UNABLE_TO_PARSE_FIELD, "value");

        // when
        error = expectError(() -> user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.BOOLEAN)));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].value", "not_null");
    }

    @Test
    public void shouldCreateDateTimeValueNode() {
        // given
        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.DATE_TIME)
                .data("value", "2020-01-01T11:22:33.444+01:00")
                .build();

        // when
        NodeResponse node = user.writeNode(request);

        // then
        assertThat(node).isNotNull();
        assertThat(node.getState()).containsEntry("value", "2020-01-01T11:22:33.444+01:00");
    }

    @Test
    public void shouldValidateDateTimeValueNode() {
        // when
        var error = expectError(() -> user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.DATE_TIME)
                .data("value", "some time tomorrow")));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.UNABLE_TO_PARSE_FIELD, "value");

        // when
        error = expectError(() -> user.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(Types.DATE_TIME)));

        // then
        assertThat(error).hasErrorCodeAndMessage(ErrorCode.VALIDATION_ERROR)
                .hasFieldError("requests[0].value", "not_null");
    }
}
