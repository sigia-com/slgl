package io.slgl.client.utils.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import io.slgl.client.node.permission.Requirement;

class DeserializerModifier extends BeanDeserializerModifier {
    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        if (Requirement.Spec.class.isAssignableFrom(beanDesc.getBeanClass())) {
            return new RequirementSpecDeserializer(deserializer);
        }
        return deserializer;
    }
}
