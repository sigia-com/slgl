package io.slgl.api.protocol;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeRequestTest {

    @Test
    public void shouldSerializeToJson() {
        // given
        NodeRequest request = new NodeRequest()
                .setId("example-id")
                .setData(ImmutableMap.of(
                        "level", "top"
                ))
                .setInlineLinks(ImmutableMap.of(
                        "#first", ImmutableList.of(
                                new NodeRequest()
                                        .setData(ImmutableMap.of(
                                                "level", "first"
                                        ))
                                .setInlineLinks(ImmutableMap.of(
                                        "#second", ImmutableList.of(
                                                new NodeRequest()
                                                        .setData(ImmutableMap.of(
                                                                "level", "second"
                                                        ))
                                        )
                                ))
                        )
                ));

        // when
        String json = UncheckedObjectMapper.MAPPER.writeValueAsString(request);

        // then
       assertThat(json)
               .contains("\"@id\" : \"example-id\"")
               .contains("\"level\" : \"top\"")
               .contains("\"#first\"")
               .contains("\"level\" : \"first\"")
               .contains("\"#second\"")
               .contains("\"level\" : \"second\"");
    }
}