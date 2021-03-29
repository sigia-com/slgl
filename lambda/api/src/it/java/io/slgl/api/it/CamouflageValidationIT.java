package io.slgl.api.it;

import io.slgl.api.it.utils.HashUtils;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.client.Types;
import io.slgl.client.camouflage.Camouflage;
import io.slgl.client.camouflage.CamouflageData;
import io.slgl.client.error.ErrorResponse;
import io.slgl.client.node.NodeRequest;
import io.slgl.client.node.NodeResponse;
import io.slgl.client.node.TypeNodeRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.slgl.api.it.assertj.SlglAssertions.assertThat;
import static io.slgl.api.it.data.TestDataUtils.generateUniqueId;
import static io.slgl.client.node.NodeTypeId.simple;

public class CamouflageValidationIT extends AbstractApiTest {

    @Test
    public void shouldCreateCamouflageNodeWithoutType() {
        // given
        Camouflage camouflage = Camouflage.builder()
                .anchor("#observersCfld", "#observers")
                .anchor("#auditorsCfld", "#auditors")
                .build();

        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .camouflage(camouflage)
                .build();

        // when
        NodeResponse node = ledger.writeNode(request);

        // then
        assertThat(node.getType()).isEqualTo(simple(Types.CAMOUFLAGE));
        assertThatCamouflageDataAreValid(node, camouflage);
    }

    @Test
    public void shouldCreateCamouflageNodeWithStandaloneType() {
        // given
        Camouflage camouflage = Camouflage.builder()
                .anchor("#observersCfld", "#observers")
                .anchor("#auditorsCfld", "#auditors")
                .build();

        NodeResponse type = ledger.writeNode(TypeNodeRequest.builder()
                .id(generateUniqueId()));

        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(type)
                .camouflage(camouflage)
                .build();

        // when
        NodeResponse node = ledger.writeNode(request);

        // then
        assertThat(node.getType()).isEqualTo(simple(Types.CAMOUFLAGE));
        assertThatCamouflageDataAreValid(node, camouflage);
        assertThat(getCamouflageData(node).getCamouflagedType()).isEqualTo(type.getId());
    }

    @Test
    public void shouldCreateCamouflageNodeWithInlineType() {
        // given
        Camouflage camouflage = Camouflage.builder()
                .anchor("#observersCfld", "#observers")
                .anchor("#auditorsCfld", "#auditors")
                .anchor("#7dkme", "#a")
                .build();

        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .stateProperties("test-property")
                        .anchor("#a"))
                .camouflage(camouflage)
                .build();

        // when
        NodeResponse node = ledger.writeNode(request);

        // then
        assertThat(node.getType()).isEqualTo(simple(Types.CAMOUFLAGE));
        assertThatCamouflageDataAreValid(node, camouflage);

        Map<?, ?> expectedType = convertToMap(request.getType());
        expectedType.remove("@type");

        assertThat(getCamouflageData(node).getCamouflagedType()).usingRecursiveComparison().isEqualTo(expectedType);
    }

    @Test
    public void shouldGenerateExpectedCamouflageHash() {
        // given
        Camouflage camouflage = Camouflage.builder()
                .anchor("#observersCfld", "#observers")
                .anchor("#auditorsCfld", "#auditors")
                .anchor("#7dkme", "#a")
                .fakeAnchors("#fake1", "#fake2", "#fake3")
                .build();

        NodeRequest request = NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .stateProperties("test-property")
                        .anchor("#a"))
                .camouflage(camouflage)
                .build();

        // when
        NodeResponse response = ledger.writeNode(request);

        // then
        String expectedHash = HashUtils.sha3_512("{\n" +
                "  \"anchors\": {\n" +
                "    \"#7dkme\": \"#a\",\n" +
                "    \"#auditorsCfld\": \"#auditors\",\n" +
                "    \"#observersCfld\": \"#observers\"\n" +
                "  },\n" +
                "  \"camouflaged_type\": {\n" +
                "    \"anchors\": [\n" +
                "      {\n" +
                "        \"id\": \"#a\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"state_properties\": [\n" +
                "      \"test-property\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"fake_anchors\": [\n" +
                "    \"#fake1\",\n" +
                "    \"#fake2\",\n" +
                "    \"#fake3\"\n" +
                "  ]\n" +
                "}\n");

        assertThat(response.getCamouflageSha3()).isEqualTo(expectedHash);
    }

    @Test
    public void shouldFailWhenCreatingCamouflagedNodeWithNonUniqueMapping() {
        // given
        Camouflage camouflage = Camouflage.builder()
                .anchor("#observersCfld", "#observers")
                .anchor("#auditorsCfld", "#auditors")
                .anchor("#7dkme", "#a")
                .anchor("#8dje4", "#a")
                .fakeAnchors("#90sudlk", "#ijslcksn", "#ijlzd").build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#a"))
                .camouflage(camouflage)));

        // then
        assertThat(error)
                .hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .messageContains("Camouflage mapping is not unique");
    }

    @Test
    public void shouldFailWhenCreatingCamouflagedNodeWithMissingMapping() {
        // given
        Camouflage camouflage = Camouflage.builder()
                .anchor("#observersCfld", "#observers")
                .anchor("#auditorsCfld", "#auditors")
                .anchor("#7dkme", "#a")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#a")
                        .anchor("#b"))
                .camouflage(camouflage)));

        // then
        assertThat(error)
                .hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .messageContains("Camouflage mapping is not consistent with type anchors");
    }

    @Test
    public void shouldFailWhenCreatingCamouflagedNodeWithExtraMapping() {
        // given
        Camouflage camouflage = Camouflage.builder()
                .anchor("#observersCfld", "#observers")
                .anchor("#auditorsCfld", "#auditors")
                .anchor("#7dkme", "#a")
                .anchor("#8ada", "#b")
                .build();

        // when
        ErrorResponse error = expectError(() -> ledger.writeNode(NodeRequest.builder()
                .id(generateUniqueId())
                .type(TypeNodeRequest.builder()
                        .anchor("#a")
                )
                .camouflage(camouflage)));

        // then
        assertThat(error)
                .hasErrorCode(ErrorCode.VALIDATION_ERROR)
                .messageContains("Camouflage mapping is not consistent with type anchors");
    }

    private static CamouflageData getCamouflageData(NodeResponse entry) {
        return UncheckedObjectMapper.MAPPER.convertValue(entry.getState().get("@camouflage"), CamouflageData.class);
    }

    private static Map<?, ?> convertToMap(Object value) {
        return UncheckedObjectMapper.MAPPER.convertValue(value, Map.class);
    }

    private static void assertThatCamouflageDataAreValid(NodeResponse entry, Camouflage camouflage) {
        CamouflageData camouflageData = getCamouflageData(entry);
        assertThat(camouflage.getAnchors().entrySet()).isEqualTo(camouflageData.getAnchors().entrySet());
        assertThat(camouflage.getFakeAnchors()).containsExactlyInAnyOrderElementsOf(camouflageData.getFakeAnchors());
    }
}
