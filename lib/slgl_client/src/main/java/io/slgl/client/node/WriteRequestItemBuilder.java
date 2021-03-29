package io.slgl.client.node;


import com.fasterxml.jackson.annotation.JsonValue;

public interface WriteRequestItemBuilder {
    @JsonValue
    WriteRequestItem build();
}
