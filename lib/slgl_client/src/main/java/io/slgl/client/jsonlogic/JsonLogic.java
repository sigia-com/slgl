package io.slgl.client.jsonlogic;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.slgl.client.utils.jackson.JsonLogicDeserializer;

@JsonDeserialize(using = JsonLogicDeserializer.class)
public interface JsonLogic {

}
