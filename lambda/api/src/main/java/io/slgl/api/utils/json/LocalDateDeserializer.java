package io.slgl.api.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {

	@Override
	public LocalDate deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
		return deserialize(jsonParser.readValueAs(String.class));
	}

	public static LocalDate deserialize(String value) {
		return DateTimeFormatter.ISO_LOCAL_DATE.parse(value, LocalDate::from);
	}
}
