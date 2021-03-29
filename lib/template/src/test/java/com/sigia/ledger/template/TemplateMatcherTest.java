package io.slgl.template;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class TemplateMatcherTest {

    static Stream<Arguments> testCases() {
        return Stream.of(
                arguments(
                        new Template("Ordered list:\n  @1 one\n  @2 two\n  @3 three\n\n"),
                        "Ordered list: @1 one @2 two @3 three "
                ),
                arguments(
                        new Template("\n* The Recipient\n"),
                        "\n● The Recipient\n"
                ),
                arguments(
                        new Template("Bullet list:\n  * apples\n  * oranges\n  * pears\n"),
                        "Bullet list: \u25CF apples \u25CF oranges \u25CF pears"
                ),
                arguments(
                        new Template("Bullet list:\n  * apples\n  * oranges\n  * pears\n"),
                        "Bullet list: apples oranges pears"
                ),
                arguments(
                        new Template("Bullet list:\n  * apples\n  * oranges\n  * pears\n", "#"),
                        "Bullet list: # apples # oranges # pears"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    void shouldMatch(Template template, String text) {
        assertThat(new TemplateMatcher(template).isMatching(text, emptyMap()))
                .as("Template %s should match text %s", template.getText(), text)
                .isTrue();
    }

}
