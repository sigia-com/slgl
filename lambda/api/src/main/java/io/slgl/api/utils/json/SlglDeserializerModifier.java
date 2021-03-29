package io.slgl.api.utils.json;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import io.slgl.api.model.PermissionEntity;
import io.slgl.api.protocol.NodeRequest;

public class SlglDeserializerModifier extends BeanDeserializerModifier {
    @SuppressWarnings("unchecked")
    @Override
    public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
        if (PermissionEntity.Requirement.class.isAssignableFrom(beanDesc.getBeanClass())) {
            return new RequirementDeserializer(deserializer);
        }
        if (NodeRequest.class.isAssignableFrom(beanDesc.getBeanClass())) {
            return new NodeRequestDeserializer((JsonDeserializer<NodeRequest>) deserializer);
        }
        return deserializer;
    }
}
