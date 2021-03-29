package io.slgl.api.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryUtilsTest {

    @Test
    public void shouldReturnSingleString() {
        // given
        String value = "foo";

        // when
        String indexValue = RepositoryUtils.buildIndexValue(value);

        // then
        assertThat(indexValue).isEqualTo(value);
    }

    @Test
    public void shouldConcatenateMultipleValues() {
        // given
        String value = "foo";
        String value2 = "bar";
        String value3 = "baz";

        // when
        String indexValue = RepositoryUtils.buildIndexValue(value, value2, value3);

        // then
        assertThat(indexValue).isEqualTo("foo|bar|baz");
    }

    @Test
    public void shouldEscapeSpecialCharacters() {
        // given
        String value = "foo|bar\\baz";

        // when
        String indexValue = RepositoryUtils.buildIndexValue(value);

        // then
        assertThat(indexValue).isEqualTo("foo\\|bar\\\\baz");
    }
}
