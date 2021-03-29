package io.slgl.api.type;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltinTypeTest {

    @Test
    public void idShouldBeUnique() {
        Set<String> idSet = new HashSet<>();

        for (BuiltinType value : BuiltinType.values()) {
            assertThat(idSet).doesNotContain(value.getId());
            idSet.add(value.getId());
        }
    }
}
