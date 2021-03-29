package io.slgl.client.utils.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;


public class SlglModule extends SimpleModule {
    {
        setDeserializerModifier(new DeserializerModifier());
        setSerializerModifier(new SerializerModifier());
    }
}
