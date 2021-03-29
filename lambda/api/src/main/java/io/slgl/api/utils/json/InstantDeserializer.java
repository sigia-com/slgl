package io.slgl.api.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class InstantDeserializer extends JsonDeserializer<Instant> {

    private static final DateTimeFormatter ISO_DATE_TIME_IN_UTC = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC);

    @Override
    public Instant deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        return deserialize(jsonParser.readValueAs(String.class));
    }

    public static Instant deserialize(String value) {
        return ISO_DATE_TIME_IN_UTC.parse(value, Instant::from);
    }
}
