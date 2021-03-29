package io.slgl.api.jsonlogic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.slgl.api.model.PermissionEntity;
import io.slgl.api.repository.NodeEntity;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.client.audit.EvaluationLogEntry;
import io.slgl.client.audit.PermissionEvaluationResult;
import io.slgl.client.node.permission.Permission;
import io.slgl.permission.PermissionProcessor;
import io.slgl.permission.context.EvaluationContext;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.slgl.client.node.permission.Requirement.requireThat;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;


public class PermissionProcessorTest {

    private PermissionProcessor permissionProcessor;

    @BeforeEach
    public void setup() {
        permissionProcessor = new PermissionProcessor();
    }

    @Test
    public void shouldReturnTrueIfRequirementForOneDomainAndUserHasThatDomain() {
        // given
        var context = buildContextWithDomains("def.com");
        var permission = new PermissionEntity()
                .setRequire(ImmutableMap.of("$api_client.verified_domains", new PermissionEntity.Requirement()
                        .setOp("contains")
                        .setValue("def.com")));

        // when
        boolean process = process(context, permission).isSuccess();

        // then
        assertThat(process).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "@($node.not.closed.bracket  |  Unexpected end of input, expecting `)`",
            "@('not.closed.bracket'      |  Unexpected end of input, expecting `)`",
            "@('not.closed.bracket'\\)   |  Unexpected end of input, expecting `)`",
            "@('escape at the end')\\    |  Unexpected end of input after escape character `\\`",
            "@('not closed string)       |  String argument not closed with `'`",
            "@('not closed string)       |  String argument not closed with `'`",
    })
    public void shouldLogButNotThrowOnVariableSyntaxError(String variable, String expectedLog) {
        // given
        var context = Map.<String, Object>of();
        var permission = Permission.builder()
                .requireAll(requireThat(variable).isEqualTo().value("ignored"))
                .build();
        // when
        var processed = process(context, permission);

        // then
        assertThat(processed)
                .matches(result -> !result.isSuccess(), "!isSuccess");
        assertThat(processed.getEvaluationLog())
                .extracting(EvaluationLogEntry::getMessage)
                .anySatisfy(it -> assertThat(it).contains(expectedLog));

    }

    @Test
    public void shouldWorkOnNestedFunctionCalls() {
        // given
        var context = Map.<String, Object>of(
                "@('id1')", Map.of("property", "argument_value"),
                "other_function('argument_value')", Map.of("link", "id3"),
                "@('id3')", Map.of("target", true)
        );
        var permission = Permission.builder()
                .requireAll(requireThat("@(other_function(@('id1').property).link).target").isEqualTo().value(true))
                .build();
        // when
        var process = process(context, permission);

        // then
        assertThat(process).matches(PermissionEvaluationResult::isSuccess, "isSuccess");

    }

    @Test
    public void shouldReturnFalseIfRequirementForOneDomainAndUserHasNoDomains() {
        // given
        var context = buildContextWithDomains(emptyList());
        var permission = new PermissionEntity()
                .setRequire(ImmutableMap.of("$api_client.verified_domains", new PermissionEntity.Requirement()
                        .setOp("contains")
                        .setValue("def.com")));

        // when
        boolean process = process(context, permission).isSuccess();

        // then
        assertThat(process).isFalse();
    }

    @Test
    public void shouldReturnFalseIfRequirementForOneDomainAndUserHasNullDomains() {
        // given
        var context = buildContextWithDomains((List<String>) null);
        var permission = new PermissionEntity()
                .setRequire(ImmutableMap.of("$api_client.verified_domains", new PermissionEntity.Requirement()
                        .setOp("contains")
                        .setValue("def.com")));

        // when
        boolean process = process(context, permission).isSuccess();

        // then
        assertThat(process).isFalse();
    }

    @Test
    public void shouldReturnTrueIfRequirementForOneDomainAndUserHasMultipleDomainsContainingRequirement() {
        // given
        var context = buildContextWithDomains("def3.com", "def.com", "def2.com");
        var permission = new PermissionEntity()
                .setRequire(ImmutableMap.of("$api_client.verified_domains", new PermissionEntity.Requirement()
                        .setOp("contains")
                        .setValue("def.com")));

        // when
        boolean process = process(context, permission).isSuccess();

        // then
        assertThat(process).isTrue();
    }

    @Test
    public void shouldReturnFalseIfRequirementForOneDomainAndUserHasMultipleDomainsNotContainingRequirement() {
        // given
        var context = buildContextWithDomains("def3.com", "def4.com", "def2.com");
        var permission = new PermissionEntity()
                .setRequire(ImmutableMap.of("$api_client.verified_domains", new PermissionEntity.Requirement()
                        .setOp("contains")
                        .setValue("def.com")));

        // when
        boolean process = process(context, permission).isSuccess();

        // then
        assertThat(process).isFalse();
    }

    @Test
    public void shouldReturnTrueIfRequirementForMultipleDomainsAndUserHasOneOfThem() {
        // given
        var context = buildContextWithDomains("def.com");
        var permission = new PermissionEntity()
                .setRequire(ImmutableMap.of("$api_client.verified_domains", new PermissionEntity.Requirement()
                        .setOp("contains_any_of")
                        .setValue(ImmutableList.of("def.com", "def2.com"))));

        // when
        boolean process = process(context, permission).isSuccess();

        // then
        assertThat(process).isTrue();
    }

    @Test
    public void shouldReturnFalseIfRequirementForMultipleDomainsAndUserHasNoneOfThem() {
        // given
        var context = buildContextWithDomains("xyz.com");
        var permission = new PermissionEntity()
                .setRequire(ImmutableMap.of("$api_client.verified_domains", new PermissionEntity.Requirement()
                        .setOp("contains_any_of")
                        .setValue(ImmutableList.of("def.com", "def2.com"))));

        // when
        boolean process = process(context, permission).isSuccess();

        // then
        assertThat(process).isFalse();
    }

    @Test
    public void shouldHandleAggregationRequirement() {
        // given
        var permission = new PermissionEntity()
                .setRequire(ImmutableMap.of("$node.values", new PermissionEntity.Requirement()
                        .setAggregate("sum")
                        .setOp("==")
                        .setVar("$node.expected_sum")));

        Map<String, Object> validContext = ImmutableMap.of("$node",
                ImmutableMap.of(
                        "values", ImmutableList.of(1, 2, 3, 4),
                        "expected_sum", 10
                ));

        Map<String, Object> invalidContext = ImmutableMap.of("$node",
                ImmutableMap.of(
                        "values", ImmutableList.of(1, 2, 3, 4),
                        "expected_sum", 42
                ));

        // when
        boolean validContextResult = process(validContext, permission).isSuccess();
        boolean invalidContextResult = process(invalidContext, permission).isSuccess();

        // then
        assertThat(validContextResult).isTrue();
        assertThat(invalidContextResult).isFalse();
    }

    @Test
    public void shouldHandleNestedRequirements() {
        // given
        var permission = new PermissionEntity()
                .setRequire(ImmutableMap.of("$node.values", new PermissionEntity.Requirement()
                        .setOp("at_least_one_meets_requirements")
                        .setValue(ImmutableMap.of(
                                "$current.string", "expected-value",
                                "$current.int", 42))));

        Map<String, Object> validContext = ImmutableMap.of("$node", ImmutableMap.of("values",
                ImmutableList.of(
                        ImmutableMap.of("string", "example-1", "int", 1),
                        ImmutableMap.of("string", "expected-value", "int", 42),
                        ImmutableMap.of("string", "example-2", "int", 2)
                )));

        Map<String, Object> invalidContext = ImmutableMap.of("$node", ImmutableMap.of("values",
                ImmutableList.of(
                        ImmutableMap.of("string", "expected-value", "int", 0),
                        ImmutableMap.of("string", "unexpected-value", "int", 42)
                )));

        // when
        boolean validContextResult = process(validContext, permission).isSuccess();
        boolean invalidContextResult = process(invalidContext, permission).isSuccess();

        // then
        assertThat(validContextResult).isTrue();
        assertThat(invalidContextResult).isFalse();
    }

    @Test
    public void shouldHandleNestedRequirementsWithCustomNameForCurrentObject() {
        // given
        var permission = new PermissionEntity()
                .setRequire(ImmutableMap.of("$node.values", new PermissionEntity.Requirement()
                        .setAs("$i")
                        .setOp("at_least_one_meets_requirements")
                        .setValue(ImmutableMap.of("$i", 42))));

        Map<String, Object> validContext = ImmutableMap.of("$node", ImmutableMap.of("values",
                ImmutableList.of(1, 42, 2)));

        Map<String, Object> invalidContext = ImmutableMap.of("$node", ImmutableMap.of("values",
                ImmutableList.of(1, 2, 3)));

        // when
        boolean validContextResult = process(validContext, permission).isSuccess();
        boolean invalidContextResult = process(invalidContext, permission).isSuccess();

        // then
        assertThat(validContextResult).isTrue();
        assertThat(invalidContextResult).isFalse();
    }


    @Test
    public void shouldHandleNodeIdReferences() {
        // given
        var id = "https://test.com/mock_" + RandomStringUtils.randomAlphanumeric(10);
        var type = "https://test.com/type__" + RandomStringUtils.randomAlphanumeric(10);

        var targetEntry = new NodeEntity()
                .setId(id)
                .setType(type);
        var targetEntryMap = UncheckedObjectMapper.MAPPER.convertValue(targetEntry, Map.class);

        //when
        var permissionByConstantId = new PermissionEntity()
                .setRequire(ImmutableMap.of("@('" + id + "').@type", new PermissionEntity.Requirement()
                        .setOp("==").setValue(type)));
        var permissionByReferencedId = new PermissionEntity()
                .setRequire(ImmutableMap.of("@($node.id_reference).@type", new PermissionEntity.Requirement()
                        .setOp("==").setValue(type)));

        Map<String, Object> validContext = Map.of(
                "@('" + id + "')", targetEntryMap,
                "$node", singletonMap(
                        "id_reference", id));
        Map<String, Object> invalidContext = Map.of(
                "$node", singletonMap(
                        "id_reference", "http://not.existing/id"));

        // then
        assertThat(process(validContext, permissionByConstantId))
                .matches(PermissionEvaluationResult::isSuccess, "isSuccess");

        assertThat(process(validContext, permissionByReferencedId))
                .matches(PermissionEvaluationResult::isSuccess, "isSuccess");

        assertThat(process(invalidContext, permissionByReferencedId))
                .matches(it -> !it.isSuccess(), "!isSuccess");
    }

    @Test
    public void shouldHandleNestedRequirementsWithReferenceToRootContext() {
        // given
        var permission = new PermissionEntity()
                .setRequire(ImmutableMap.of("$api_client.verified_domains", new PermissionEntity.Requirement()
                        .setOp("at_least_one_meets_requirements")
                        .setValue(ImmutableMap.of(
                                "$current", ImmutableMap.of(
                                        "var", "$node.domain")))));

        Map<String, Object> validContext = buildContextWithDomains("foo.com", "bar.com", "baz.com");
        validContext.put("$node", ImmutableMap.of("domain", "bar.com"));

        Map<String, Object> invalidContext = buildContextWithDomains("foo.com", "bar.com", "baz.com");
        invalidContext.put("$node", ImmutableMap.of("domain", "some-other-domain.com"));

        // when
        boolean validContextResult = process(validContext, permission).isSuccess();
        boolean invalidContextResult = process(invalidContext, permission).isSuccess();

        // then
        assertThat(validContextResult).isTrue();
        assertThat(invalidContextResult).isFalse();
    }

    @Test
    public void shouldProcessPermissionsInJsonLogic() {
        // given
        var permissionAlwaysAllow = new PermissionEntity()
                .setRequireLogic(true);
        var permissionAlwaysDeny = new PermissionEntity()
                .setRequireLogic(false);

        Map<String, Object> context = Collections.emptyMap();

        // when
        boolean alwaysAllowResult = process(context, permissionAlwaysAllow).isSuccess();
        boolean alwaysDenyResult = process(context, permissionAlwaysDeny).isSuccess();

        // then
        assertThat(alwaysAllowResult).isTrue();
        assertThat(alwaysDenyResult).isFalse();
    }


    private PermissionEvaluationResult process(Map<String, ?> context, Object permission) {
        return permissionProcessor.process(EvaluationContext.of(context), UncheckedObjectMapper.MAPPER.convertValue(permission, Permission.class));
    }

    private Map<String, Object> buildContextWithDomains(String... domains) {
        return buildContextWithDomains(ImmutableList.copyOf(domains));
    }

    private Map<String, Object> buildContextWithDomains(List<String> domains) {
        var userMapBuilder = ImmutableMap.<String, Object>builder();
        if (domains != null) {
            userMapBuilder.put("verified_domains", domains);
        }
        var context = new HashMap<String, Object>();
        context.put("$api_client", userMapBuilder.build());
        return context;
    }
}
