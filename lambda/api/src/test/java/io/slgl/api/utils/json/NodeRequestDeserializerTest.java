package io.slgl.api.utils.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.slgl.api.protocol.ApiRequest;
import io.slgl.api.protocol.ApiRequestItem;
import io.slgl.api.protocol.NodeRequest;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

class NodeRequestDeserializerTest {

    @Test
    public void shouldDeserializeNodeRequest() {
        // given
        String firstNodeJson = "{\n  \"@id\" : \"first\"\n}";
        String secondNodeJson = "{\n  \"@id\" : \"second\"\n}";

        String json = "{\n" +
                "  \"requests\" : [\n" +
                "    {\n" +
                "      \"node\" :" + firstNodeJson + "\n" +
                "    },\n" +
                "    {\n" +
                "      \"node\" :" + secondNodeJson + "\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        // when
        ApiRequest testObject = UncheckedObjectMapper.MAPPER.readValue(json, ApiRequest.class);

        // then
        assertThat(testObject.getRequests())
                .asInstanceOf(list(NodeRequest.class))
                .hasSize(2)
                .satisfies(
                        item -> {
                            assertThat(item.getId()).isEqualTo("first");
                            assertThat(item.getRawJson()).isEqualToNormalizingNewlines(firstNodeJson);
                        },
                        atIndex(0))
                .satisfies(
                        item -> {
                            assertThat(item.getId()).isEqualTo("second");
                            assertThat(item.getRawJson()).isEqualToNormalizingNewlines(secondNodeJson);
                        },
                        atIndex(1));
    }

    @Data
    public static class TestObject {

        @JsonDeserialize(contentUsing = NodeRequestDeserializer.class)
        @JsonIgnore
        private List<NodeRequest> nodes;

        @JsonProperty("nodes")
        private List<ApiRequestItem> nodes2;
    }
}