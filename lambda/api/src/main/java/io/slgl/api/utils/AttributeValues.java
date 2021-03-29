package io.slgl.api.utils;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collection;

public abstract class AttributeValues {


    public static AttributeValue s(String value) {
        return AttributeValue.builder().s(value).build();
    }

    public static AttributeValue ss(Collection<String> values) {
        return AttributeValue.builder().ss(values).build();
    }

    public static AttributeValue n(Number value) {
        return AttributeValue.builder().n(value.toString()).build();
    }

    public static AttributeValue ss(String... values) {
        return AttributeValue.builder().ss(values).build();
    }

    public static AttributeValue bool(boolean value) {
        return AttributeValue.builder().bool(value).build();
    }
}
