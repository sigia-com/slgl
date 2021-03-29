package io.slgl.api.observer.model;

import io.slgl.api.utils.json.UncheckedObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ObserverEntityTest {

    @Test
    public void shouldDeserializeObserverWithS3Storage() {
        //given
        String json = "{\n"
            + "    \"pgp_key\" : \"public pgp key\",\n"
            + "    \"aws_s3\" : {\n"
            + "      \"bucket\" : \"bucket-name\",\n"
            + "      \"region\" : \"eu-west-1\",\n"
            + "      \"path\" : \"path-inside-bucket\",\n"
            + "      \"credentials\" : {\n"
            + "        \"access_key_id\" : \"key_id\",\n"
            + "        \"secret_access_key\" : \"secret_key\"\n"
            + "      }\n"
            + "    }\n"
            + "  }";

        //when
        ObserverEntity observerEntity = UncheckedObjectMapper.MAPPER
            .readValue(json, ObserverEntity.class);

        //than
        assertThat(observerEntity.getS3Storage()).isInstanceOf(S3Storage.class);
    }
}