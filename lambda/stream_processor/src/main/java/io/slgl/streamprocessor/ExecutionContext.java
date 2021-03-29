package io.slgl.streamprocessor;

import com.amazon.ion.system.IonSystemBuilder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.ion.ionvalue.IonValueMapper;
import software.amazon.awssdk.services.sns.SnsClient;

public class ExecutionContext {

    private static IonValueMapper ionMapper;
    private static ObjectMapper objectMapper;

    private static SnsClient amazonSNS;

    static {
        ionMapper = new IonValueMapper(IonSystemBuilder.standard().build());
        ionMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        ionMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        amazonSNS = SnsClient.create();
    }

    public static IonValueMapper getIonMapper() {
        return ionMapper;
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static SnsClient getAmazonSNS() {
        return amazonSNS;
    }
}
