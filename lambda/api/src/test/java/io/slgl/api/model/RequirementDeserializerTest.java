package io.slgl.api.model;

import io.slgl.api.model.PermissionEntity.Requirements;
import io.slgl.api.model.PermissionEntity.Requirements.RequirementsList;
import io.slgl.api.model.PermissionEntity.Requirements.SimpleRequirements;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import lombok.Value;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

public class RequirementDeserializerTest {

    public static Stream<Arguments> testCases() {
        return Stream.of(
                // simple test cases
                testCase("{\"path\": {\"op\": \">\", \"value\": \"example\"}}",
                        expectSimple(),
                        expectRequirement(">", "example")),
                testCase("{\"path\": \"example\"}",
                        expectSimple(),
                        expectRequirement(null, "example")),
                testCase("{\"path\": 123}",
                        expectSimple(),
                        expectRequirement(null, 123)),

                // the same but embedded in list
                testCase("[{\"path\": {\"op\": \">\", \"value\": \"example\"}}]",
                        expectList(),
                        expectRequirement(">", "example")),
                testCase("[{\"path\": \"example\"}]",
                        expectList(),
                        expectRequirement(null, "example")),
                testCase("[{\"path\": 123}]",
                        expectList(),
                        expectRequirement(null, 123)),

                // multiple requirements on same path
                testCase("[{\"path\": \"first\"}, {\"path\":  \"second\"}]",
                        expectList(),
                        expectRequirement(null, "first"),
                        expectRequirement(null, "second")),

                // multiple requirements on same path of different requirement structure
                testCase("[{\"path\": \"first\"}, {\"path\":  2}, {\"path\": {\"value\": {\"nested\":\"third\"}}}]",
                        expectList(),
                        expectRequirement(null, "first"),
                        expectRequirement(null, 2),
                        expectRequirement(null, singletonMap("nested", "third")))
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void shouldDeserializeRequirementsList(String json, Class<?> expectedRequirementClass, List<ExpectedRequirement> expectedRequirements) {
        // when
        Requirements require = UncheckedObjectMapper.MAPPER.readValue(json, Requirements.class);

        // then
        assertThat(require).isInstanceOf(expectedRequirementClass);
        assertThat(require.get("path"))
                .zipSatisfy(expectedRequirements, (requirement, expected) -> {
                    assertThat(requirement.getOp()).isEqualTo(expected.op);
                    assertThat(requirement.getValue()).isEqualTo(expected.value);
                });
    }

    public static Arguments testCase(String json, Class<? extends Requirements> simpleRequirementsClass, ExpectedRequirement... requirements) {
        return Arguments.of(json, simpleRequirementsClass, Arrays.asList(requirements));
    }

    public static Class<RequirementsList> expectList() {
        return RequirementsList.class;
    }

    public static Class<SimpleRequirements> expectSimple() {
        return SimpleRequirements.class;
    }

    public static ExpectedRequirement expectRequirement(String op, Object value) {
        return new ExpectedRequirement(op, value);
    }

    @Value
    public static class ExpectedRequirement {
        String op;
        Object value;
    }
}
