package io.slgl.api.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Objects;
import io.slgl.api.ExecutionContext;
import io.slgl.api.config.Provider;
import io.slgl.api.error.ApiException;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.S3ClientFactory;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.slgl.api.utils.Utils.getSha3OnJqSCompliantJson;
import static io.slgl.api.utils.json.UncheckedObjectMapper.MAPPER;

@Slf4j
public class StateRepository {

    private final Provider<S3Client> s3Client = ExecutionContext.getProvider(S3Client.class);
    private final S3ClientFactory s3ClientFactory = ExecutionContext.get(S3ClientFactory.class);
    private final Map<String, Map<String, Object>> stateFromRequest = new HashMap<>();

    public Optional<Map<String, Object>> readState(NodeEntity node) {

        var state = stateFromRequest.get(node.getId());
        if (state != null) {
            verifyStateMatchesStateHashFromNode(state, node);
            return Optional.of(state);
        }

        if (node.getStateSource() != null) {
            try {
                byte[] stateBytes = loadStateFromStateSource(node.getStateSource());

                state = UncheckedObjectMapper.MAPPER.readValue(new ByteArrayInputStream(stateBytes), new TypeReference<>() {});
                verifyStateMatchesStateHashFromNode(state, node);

                return Optional.of(state);

            } catch (Exception e) {
                log.info("Unable to read state form state source: {}", node.getStateSource(), e);
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private byte[] loadStateFromStateSource(String stateSourceString) throws IOException {
        StateSource stateSource = StateSource.parse(stateSourceString);
        if (stateSource == null) {
            throw new IllegalArgumentException("Unable to parse state source: " + stateSourceString);
        }

        switch (stateSource.getScheme()) {
            case "s3":
                Region region = getS3BucketRegion(stateSource);

                GetObjectRequest request = GetObjectRequest.builder()
                        .bucket(stateSource.getHost())
                        .key(stateSource.getPath())
                        .build();

                return s3ClientFactory.getS3Client(region).getObject(request).readAllBytes();

            default:
                throw new IllegalArgumentException("Unsupported state source scheme: " + stateSource.getScheme());
        }
    }

    private Region getS3BucketRegion(StateSource stateSource) {
        GetBucketLocationResponse bucketLocation = s3Client.get().getBucketLocation(GetBucketLocationRequest.builder()
                .bucket(stateSource.getHost())
                .build());

        if (bucketLocation.locationConstraint() != null) {
            return Region.of(bucketLocation.locationConstraint().toString());
        } else {
            return Region.US_EAST_1;
        }
    }

    private void verifyStateMatchesStateHashFromNode(Object state, NodeEntity node) {
        String stateFromRequestHash = getSha3OnJqSCompliantJson(MAPPER.writeValueAsString(state));

        if (!Objects.equal(node.getStateSha3(), stateFromRequestHash)) {
            throw new ApiException(ErrorCode.STATE_HASH_MISMATCH, node.getId());
        }
    }

    public void putStateFromRequest(String nodeId, Map<String, Object> state) {
        stateFromRequest.put(nodeId, state);
    }

    @AllArgsConstructor
    @Getter
    public static class StateSource {

        private static final Pattern STATE_SOURCE_PATTERN = Pattern.compile("(?<scheme>[^:/]+)://(?<host>[^:/]*)/(?<path>.*)");

        private final String scheme;
        private final String host;
        private final String path;

        public static StateSource parse(String stateSource) {
            Matcher matcher = STATE_SOURCE_PATTERN.matcher(stateSource);
            if (!matcher.matches()) {
                return null;
            }

            return new StateSource(
                    matcher.group("scheme"),
                    matcher.group("host"),
                    matcher.group("path"));
        }
    }
}
