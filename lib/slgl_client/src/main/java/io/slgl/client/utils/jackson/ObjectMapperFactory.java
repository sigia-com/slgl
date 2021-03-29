package io.slgl.client.utils.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

public abstract class ObjectMapperFactory {
    private ObjectMapperFactory() {
    }

    public static ObjectMapper createSlglObjectMapper() {
        return new ObjectMapper()
                .enable(INDENT_OUTPUT)
                .registerModules(new JavaTimeModule(), new SlglModule());
    }
}
