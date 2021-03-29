package io.slgl.api.utils;

import org.junit.jupiter.api.Test;

import static io.slgl.api.utils.Utils.concatenateAsUrlPartsWithSlash;
import static org.assertj.core.api.Assertions.assertThat;

public class UtilsTest {
    @Test
    public void shouldConcatenateUrlPaths() {
        assertThat(concatenateAsUrlPartsWithSlash("http://www.abc.com", "a", "b", "c")).isEqualTo("http://www.abc.com/a/b/c");
    }

    @Test
    public void shouldConcatenateUrlPathsAndRemoveAdditionalSlashes() {
        assertThat(concatenateAsUrlPartsWithSlash("http://www.abc.com/", "a", "b//", "c", "/d")).isEqualTo("http://www.abc.com/a/b/c/d");
    }

    @Test
    public void shouldOmitNullParts() {
        assertThat(concatenateAsUrlPartsWithSlash("http://www.abc.com/", "a", null, "c", "/d", null)).isEqualTo("http://www.abc.com/a/c/d");
    }
}
