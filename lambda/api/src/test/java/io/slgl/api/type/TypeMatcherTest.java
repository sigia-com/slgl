package io.slgl.api.type;

import io.slgl.api.model.TypeEntity;
import io.slgl.api.model.TypeEntity.AnchorEntity;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.client.jsonlogic.And;
import io.slgl.client.jsonlogic.Equal;
import io.slgl.client.jsonlogic.Var;
import io.slgl.client.node.TemplateNodeRequest;
import io.slgl.client.node.TypeNodeRequest;
import io.slgl.client.node.permission.Allow;
import io.slgl.client.node.permission.Permission;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.slgl.api.type.TypeMatcherTest.TestCase.testCase;
import static io.slgl.api.utils.CollectionUtils.nullToEmptyList;
import static io.slgl.client.node.permission.Requirement.requireThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class TypeMatcherTest {

    private TypeMatcher typeMatcher = new TypeMatcher();

    public static Stream<TestCase> positiveTestCases() {
        return Stream.of(
                testCase("more anchors")
                        .baseType(TypeNodeRequest.builder()
                                .anchor("#a"))
                        .extendingType(TypeNodeRequest.builder()
                                .anchor("#a")
                                .anchor("#b")),
                testCase("more state properties")
                        .baseType(TypeNodeRequest.builder()
                                .stateProperties("a"))
                        .extendingType(TypeNodeRequest.builder()
                                .stateProperties("a", "b")),
                testCase("more templates")
                        .baseType(TypeNodeRequest.builder()
                                .linkTemplate(TemplateNodeRequest.builder()
                                        .text("tempalte text alfa")))
                        .extendingType(TypeNodeRequest.builder()
                                .linkTemplate(TemplateNodeRequest.builder()
                                        .text("tempalte text alfa"))
                                .linkTemplate(TemplateNodeRequest.builder()
                                        .text("tempalte text beta"))),
                testCase("anchor with type with more state properties")
                        .baseType(TypeNodeRequest.builder()
                                .anchor("#a", TypeNodeRequest.builder()
                                        .stateProperties("x")))
                        .extendingType(TypeNodeRequest.builder()
                                .anchor("#a", TypeNodeRequest.builder()
                                        .stateProperties("x", "y"))),
                testCase("more permissions with different allow")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .alwaysAllowed()))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .alwaysAllowed())
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#b"))
                                        .alwaysAllowed())),
                testCase("multiple permissions with same allow but in different order")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path-1").isEqualTo().value("value-1")))
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path-2").isEqualTo().value("value-2")))
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path-3").isEqualTo().value("value-3")))
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path-4").isEqualTo().value("value-4"))))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path-4").isEqualTo().value("value-4")))
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path-1").isEqualTo().value("value-1")))
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path-3").isEqualTo().value("value-3")))
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path-2").isEqualTo().value("value-2")))),
                testCase("more allow in single permissions")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .alwaysAllowed()))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .allow(Allow.allowLink("#b"))
                                        .alwaysAllowed())),
                testCase("permissions with more requirements")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path").isEqualTo().value("value"))))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(
                                                requireThat("path").isEqualTo().value("value"),
                                                requireThat("other-path").isEqualTo().value("other-value")))),
                testCase("permissions with more requirements (when base permission is always allowed)")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .alwaysAllowed()))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(
                                                requireThat("path").isEqualTo().value("value")))),
                testCase("permissions with more require logic")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireLogic(new Equal(new Var("path"), "value"))))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireLogic(new And(
                                                new Equal(new Var("path"), "value"),
                                                new Equal(new Var("other-path"), "other-value"))))),
                testCase("permissions with more require logic (when base permission is always allowed)")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireLogic(true)))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireLogic(new Equal(new Var("path"), "value"))))
        );
    }

    public static Stream<TestCase> negativeTestCases() {
        return Stream.of(
                testCase("different anchors")
                        .baseType(TypeNodeRequest.builder()
                                .anchor("#a"))
                        .extendingType(TypeNodeRequest.builder()
                                .anchor("#b")),
                testCase("anchor with different max size")
                        .baseType(TypeNodeRequest.builder()
                                .anchor("#a"))
                        .extendingType(TypeNodeRequest.builder()
                                .anchor("#a", 1)),
                testCase("different state properties")
                        .baseType(TypeNodeRequest.builder()
                                .stateProperties("a"))
                        .extendingType(TypeNodeRequest.builder()
                                .stateProperties("b")),
                testCase("different templates")
                        .baseType(TypeNodeRequest.builder()
                                .linkTemplate(TemplateNodeRequest.builder()
                                        .text("tempalte text alfa")))
                        .extendingType(TypeNodeRequest.builder()
                                .linkTemplate(TemplateNodeRequest.builder()
                                        .text("tempalte text beta"))),
                testCase("anchor with type with different state properties")
                        .baseType(TypeNodeRequest.builder()
                                .anchor("#a", TypeNodeRequest.builder()
                                        .stateProperties("x")))
                        .extendingType(TypeNodeRequest.builder()
                                .anchor("#a", TypeNodeRequest.builder()
                                        .stateProperties("y"))),
                testCase("permissions with different allows")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .alwaysAllowed()))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#b"))
                                        .alwaysAllowed())),
                testCase("permissions with different requirements")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path").isEqualTo().value("value"))))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .alwaysAllowed())),
                testCase("permissions with different requirements (different value)")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path").isEqualTo().value("value"))))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path").isEqualTo().value("other-value")))),
                testCase("permissions with different requirements (different path)")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path").isEqualTo().value("value"))))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("other-path").isEqualTo().value("value")))),
                testCase("permissions with different requirements (different operator)")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path").isEqualTo().value("value"))))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireAll(requireThat("path").isNotEqualTo().value("value")))),
                testCase("permissions with different require logic")
                        .baseType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireLogic(new Equal(new Var("path"), "value"))))
                        .extendingType(TypeNodeRequest.builder()
                                .permission(Permission.builder()
                                        .allow(Allow.allowLink("#a"))
                                        .requireLogic(new Equal(new Var("other-path"), "other-value"))))
        );
    }

    @ParameterizedTest
    @MethodSource({
            "positiveTestCases"
    })
    void shouldReturnTrueWhenTypeIsExtendingBaseType(TestCase testCase) {
        // when
        boolean result = typeMatcher.isEqualOrExtendingType(testCase.baseType, testCase.extendingType);

        // then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @MethodSource({
            "negativeTestCases"
    })
    void shouldReturnFalseWhenTypeIsNotExtendingBaseType(TestCase testCase) {
        // when
        boolean result = typeMatcher.isEqualOrExtendingType(testCase.baseType, testCase.extendingType);

        // then
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @MethodSource({
            "positiveTestCases"
    })
    void shouldReturnTrueWhenCheckingIfTypeIsExtendingItself(TestCase testCase) {
        // when
        boolean result = typeMatcher.isEqualOrExtendingType(testCase.baseType, testCase.baseTypeClone);

        // then
        assertThat(result).isTrue();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static Type mockType(String id, TypeEntity entity) {
        Type type = mock(Type.class, new ExceptionAnswer());

        doReturn(id).when(type).getId();
        doReturn(nullToEmptyList(entity.getStateProperties())).when(type).getStateProperties();
        doReturn(nullToEmptyList(entity.getTemplates())).when(type).getTemplates();
        doReturn(nullToEmptyList(entity.getPermissions())).when(type).getPermissions();

        List<Anchor> anchors = nullToEmptyList(entity.getAnchors()).stream()
                .map(TypeMatcherTest::mockAnchor)
                .collect(Collectors.toList());
        doReturn(anchors).when(type).getAnchors();

        doReturn(false).when(type).isOrExtendsType(anyString());

        return type;
    }

    private static Anchor mockAnchor(AnchorEntity entity) {
        Anchor anchor = mock(Anchor.class, new ExceptionAnswer());

        doReturn(entity.getId()).when(anchor).getId();
        doReturn(entity.getMaxSize()).when(anchor).getMaxSize();

        if (entity.getInlineType() != null) {
            TypeEntity inlineTypeEntity = UncheckedObjectMapper.MAPPER.convertValue(entity.getInlineType(), TypeEntity.class);
            Type inlineType = mockType(null, inlineTypeEntity);
            doReturn(Optional.of(inlineType)).when(anchor).getType();
        } else {
            doReturn(Optional.empty()).when(anchor).getType();
        }

        return anchor;
    }

    @Data
    @Accessors(fluent = true, chain = true)
    static class TestCase {

        private String name;
        private Type baseType;
        private Type baseTypeClone;
        private Type extendingType;

        public static TestCase testCase(String description) {
            return new TestCase()
                    .name(description);
        }

        public TestCase baseType(TypeNodeRequest.Builder typeBuilder) {
            TypeEntity typeEntity = UncheckedObjectMapper.MAPPER.convertValue(typeBuilder.build(), TypeEntity.class);
            baseType = mockType("base_type", typeEntity);

            TypeEntity otherTypeEntity = UncheckedObjectMapper.MAPPER.convertValue(typeBuilder.build(), TypeEntity.class);
            baseTypeClone = mockType("base_type_clone", otherTypeEntity);

            return this;
        }

        public TestCase extendingType(TypeNodeRequest.Builder typeBuilder) {
            TypeEntity typeEntity = UncheckedObjectMapper.MAPPER.convertValue(typeBuilder.build(), TypeEntity.class);
            extendingType = mockType("extending_type", typeEntity);
            return this;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class ExceptionAnswer implements Answer<Object> {
        public Object answer(InvocationOnMock invocation) {
            throw new UnsupportedOperationException(invocation.getMethod().getName() + " is not stubbed");
        }
    }
}