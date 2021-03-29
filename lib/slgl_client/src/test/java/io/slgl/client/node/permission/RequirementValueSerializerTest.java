package io.slgl.client.node.permission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.slgl.client.utils.jackson.ObjectMapperFactory;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static org.assertj.core.api.Assertions.assertThat;

public class RequirementValueSerializerTest {

    @Test
    public void testSerialization() throws JsonProcessingException {
        ObjectMapper mapper = ObjectMapperFactory.createSlglObjectMapper()
                .copy()
                .disable(INDENT_OUTPUT);

        Requirement requirement = Requirement.requireThat("a").isBefore().value(OffsetDateTime.of(2019, 1, 2, 3, 4, 5, 0, ZoneOffset.ofHours(-6)));

        assertThat(mapper.writeValueAsString(requirement.getRequirement()))
                .isEqualTo("{\"op\":\"before\",\"value\":\"2019-01-02T03:04:05-06:00\"}");

    }
}