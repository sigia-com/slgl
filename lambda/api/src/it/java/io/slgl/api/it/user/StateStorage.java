package io.slgl.api.it.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.slgl.api.it.data.TestDataUtils;
import io.slgl.client.SlglRequestListener;
import io.slgl.client.SlglResponseListener;
import io.slgl.client.node.*;
import io.slgl.client.utils.jackson.ObjectMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.slgl.api.it.properties.Props.getSlglProperties;

@Slf4j
public class StateStorage implements SlglRequestListener, SlglResponseListener {

    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    public static final StateStorage INSTANCE = new StateStorage();

    private final S3Client s3Client = S3Client.builder()
            .region(Region.of(getSlglProperties().getStateStorageS3Region()))
            .build();

    private final String s3Bucket = getSlglProperties().getStateStorageS3Bucket();

    private final String stateSourcePrefix = "s3://" + s3Bucket + "/";

    private final ObjectMapper mapper = ObjectMapperFactory.createSlglObjectMapper();

    private final Set<String> ignoreNodeIds = new HashSet<>();
    private final Set<WriteRequest> ignoreRequests = new HashSet<>();

    @Override
    public WriteRequest onWriteRequest(WriteRequest request) {
        if (request.getRequests() == null || request.getRequests().isEmpty() || ignoreRequests.contains(request)) {
            return request;
        }

        WriteRequest.Builder requestBuilder = WriteRequest.builder();

        for (WriteRequestItem requestItem : request.getRequests()) {

            if (requestItem instanceof NodeRequest) {
                NodeRequest nodeRequest = (NodeRequest) requestItem;

                if (nodeRequest.getStateSource() == null && !ignoreNodeIds.contains(nodeRequest.getId())) {
                    requestItem = nodeRequest.toBuilder()
                            .stateSource(generateUniqueStateSource())
                            .build();
                }
            }

            requestBuilder.addRequest(requestItem);
        }

        requestBuilder.addExistingNodeState(request.getExistingNodesState());
        requestBuilder.addExistingNodeRequests(request.getExistingNodesRequests());

        return requestBuilder.build();
    }

    @Override
    public void onWriteResponse(WriteResponse response) {
        if (response.getNodes() == null) {
            return;
        }

        for (NodeResponse node : response.getNodes()) {
            if (node.getState() != null && isSupportedStateSource(node)) {
                storeState(node, node.getState());
            }
        }
    }

    public void storeState(NodeResponse node, Object state) {
        Preconditions.checkArgument(isSupportedStateSource(node));

        String s3Key = StringUtils.removeStart(node.getStateSource(), stateSourcePrefix);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Key)
                .contentType(JSON_CONTENT_TYPE)
                .build();

        RequestBody requestBody = RequestBody.fromBytes(toJsonBytes(state));

        log.info("Storing state for node: node-id={}, bucket={}, key={}", node.getId(), s3Bucket, s3Key);
        s3Client.putObject(putObjectRequest, requestBody);
    }

    private boolean isSupportedStateSource(NodeResponse node) {
        return node.getStateSource() != null && node.getStateSource().startsWith(stateSourcePrefix);
    }

    private byte[] toJsonBytes(Object object) {
        if (object == null) {
            return null;
        }

        try {
            String json = mapper.writeValueAsString(object);
            return json.getBytes(Charsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateUniqueStateSource() {
        return stateSourcePrefix + TestDataUtils.generateUniqueStateSourceId() + ".json";
    }

    public static void ignoreStateStorageForNode(String nodeId) {
        checkNotNull(nodeId);

        INSTANCE.ignoreNodeIds.add(nodeId);
    }

    public static void ignoreStateStorageForRequest(WriteRequest request) {
        checkNotNull(request);

        INSTANCE.ignoreRequests.add(request);
    }
}
