package io.slgl.template;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TextNormalizerTest {

    @Test
    public void normalizeShouldChangeAllLettersToLowerCase() {
        // given
        String text = "foo BAR BaZ";

        // when
        String normalizedText = TextNormalizer.normalize(text);

        // then
        assertThat(normalizedText).isEqualTo("foo bar baz");
    }

    @Test
    public void normalizeShouldRemoveWhitespacesFromBeginAndEnd() {
        // given
        String text = "\n\t foo bar \n\t";

        // when
        String normalizedText = TextNormalizer.normalize(text);

        // then
        assertThat(normalizedText).isEqualTo("foo bar");
    }

    @Test
    public void normalizeShouldCompressMultipleWhitespacesToSingleSpace() {
        // given
        String text = "foo   bar \n\tbaz";

        // when
        String normalizedText = TextNormalizer.normalize(text);

        // then
        assertThat(normalizedText).isEqualTo("foo bar baz");
    }

    @Test
    public void normalizeShouldRemoveWhitespacesWhenNotBetweenWords() {
        // given
        String text = "foo\u200B (bar) , baz . ! ?";

        // when
        String normalizedText = TextNormalizer.normalize(text);

        // then
        assertThat(normalizedText).isEqualTo("foo(bar),baz.!?");
    }

    @Test
    public void normalizeShouldConvertFancyQuotesToTheirAsciiEquivalent() {
        // given
        String text = "\u201Cfoo\u201D,\u2018bar\u2019,\u00ABbaz\u00BB";

        // when
        String normalizedText = TextNormalizer.normalize(text);

        // then
        assertThat(normalizedText).isEqualTo("\"foo\",'bar',\"baz\"");
    }
}
