package io.slgl.api.utils.json;

import com.fasterxml.jackson.databind.module.SimpleModule;

public class SlglModule extends SimpleModule {
    {
        setDeserializerModifier(new SlglDeserializerModifier());
    }
}
