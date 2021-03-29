package io.slgl.api.it;

import io.slgl.api.it.properties.Props;
import io.slgl.api.utils.ErrorCode;
import io.slgl.client.SlglApiClient;
import io.slgl.client.Types;
import org.junit.jupiter.api.Test;

import static io.slgl.api.it.assertj.SlglAssertions.assertApiException;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.api.it.utils.CleanerHolder.registerInCleaner;

public class ErrorIT extends AbstractApiTest {

    @Test
    public void shouldHandleMissingApiKey() {
        // given
        SlglApiClient apiClient = SlglApiClient.builder()
                .apiUrl(Props.getSlglProperties().getLedgerUrl())
                .build();

        // then
        assertApiException(() -> apiClient.write("{}"))
                .hasErrorCode(ErrorCode.INVALID_API_KEY);
    }

    @Test
    public void shouldHandleInvalidApiKey() {
        // given
        SlglApiClient apiClient = testClient(user.getUsername(), "invalid-api-key");

        // then
        assertApiException(() -> apiClient.write("{}"))
                .hasErrorCode(ErrorCode.INVALID_API_KEY);
    }

    @Test
    public void shouldHandleInvalidJson() {
        // given
        String request = "{\"unparseable-request}}";

        // when
        assertApiException(() -> testClient().write(request))
                .hasErrorCode(ErrorCode.UNABLE_TO_PARSE_REQUEST);
    }

    @Test
    public void shouldHandleUnrecognizedField() {
        // given
        String id = generateUniqueId();
        String type = Types.TYPE;
        String request = "{\n" +
                "  \"requests\": [{\n" +
                "    \"node\": {\n" +
                "      \"@id\": \"" + id + "\",\n" +
                "      \"@type\": \"" + type + "\",\n" +
                "      \"not-existing-field\": 42\n" +
                "    }\n" +
                "  }]\n" +
                "}\n";

        // then
        assertApiException(() -> testClient().write(request))
                .hasErrorCode(ErrorCode.UNABLE_TO_PARSE_UNRECOGNIZED_FIELD, "not-existing-field");
    }

    @Test
    public void shouldHandleInvalidField() {
        // given
        String request = "{\n" +
                "  \"requests\": [{\n" +
                "    \"node\": {\n" +
                "      \"@id\": {\"not-a-string\": true}\n" +
                "    }\n" +
                "  }]\n" +
                "}\n";

        // when
        assertApiException(() -> testClient().write(request))
                .hasErrorCode(ErrorCode.UNABLE_TO_PARSE_FIELD, "requests[0].@id");

    }

    private SlglApiClient testClient() {
        return testClient(user.getUsername(), user.getSecretKey());
    }

    private SlglApiClient testClient(String username, String apiKey) {
        var client = SlglApiClient.builder()
                .apiUrl(Props.getSlglProperties().getLedgerUrl())
                .username(username)
                .apiKey(apiKey)
                .build();
        return registerInCleaner(client);
    }
}
