package io.slgl.client.utils.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import io.slgl.client.node.permission.Requirement;

class SerializerModifier extends BeanSerializerModifier {
    @SuppressWarnings("unchecked")
    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
        if (Requirement.Spec.class.isAssignableFrom(beanDesc.getBeanClass())) {
            return new RequirementSpecSerializer((JsonSerializer<Requirement.Spec>) serializer);
        }
        return serializer;
    }
}
