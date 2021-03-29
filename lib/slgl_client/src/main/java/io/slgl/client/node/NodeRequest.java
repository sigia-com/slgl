package io.slgl.client.node;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import io.slgl.client.camouflage.Camouflage;
import io.slgl.client.protocol.Identified;
import io.slgl.client.utils.Preconditions;
import io.slgl.client.utils.jackson.ObjectMapperFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonPropertyOrder({"@id", "@type"})
@JsonInclude(NON_NULL)
@JsonTypeName("node")
public class NodeRequest implements WriteRequestItem {

    @JsonProperty("@id")
    private final String id;

    @JsonProperty("@type")
    private final Object type;

    @JsonProperty("@state_source")
    private String stateSource;

    @JsonProperty("@file")
    private final byte[] file;

    @JsonProperty("@camouflage")
    private final Camouflage camouflage;

    @JsonProperty("@authorizations")
    @JsonInclude(NON_EMPTY)
    private final List<String> authorizations;

    @JsonIgnore
    private final Map<String, Object> data;

    protected NodeRequest(Builder<?> builder) {
        id = builder.id;
        type = builder.type;
        stateSource = builder.stateSource;
        file = builder.file;
        camouflage = builder.camouflage;
        authorizations = Collections.unmodifiableList(new ArrayList<>(builder.authorizations));
        data = Collections.unmodifiableMap(new LinkedHashMap<>(builder.data));
    }

    public String getId() {
        return this.id;
    }

    public Object getType() {
        return this.type;
    }

    public String getStateSource() {
        return stateSource;
    }

    public byte[] getFile() {
        return file;
    }

    public Camouflage getCamouflage() {
        return camouflage;
    }

    public List<String> getAuthorizations() {
        return authorizations;
    }

    @JsonAnyGetter
    public Map<String, Object> getData() {
        return this.data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeRequest)) return false;
        NodeRequest that = (NodeRequest) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(type, that.type) &&
                Objects.equals(stateSource, that.stateSource) &&
                Arrays.equals(file, that.file) &&
                Objects.equals(camouflage, that.camouflage) &&
                Objects.equals(authorizations, that.authorizations) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, stateSource, file, camouflage, authorizations, data);
    }

    public Builder<?> toBuilder() {
        return new Builder<>(this);
    }

    public static Builder<?> builder() {
        return new Builder<>();
    }

    public static class Builder<SELF extends Builder<SELF>> implements WriteRequestItemBuilder {

        private String id;
        private Object type;
        private String stateSource;
        private byte[] file;
        private Camouflage camouflage;
        private List<String> authorizations = new ArrayList<>();
        private Map<String, Object> data = new LinkedHashMap<>();

        public Builder() {
        }

        public Builder(NodeRequest request) {
            id = request.id;
            type = request.type;
            stateSource = request.stateSource;
            file = request.file;
            camouflage = request.camouflage;
            authorizations.addAll(request.authorizations);
            data.putAll(request.data);
        }

        public SELF id(String id) {
            this.id = id;
            return self();
        }

        public SELF type(String type) {
            this.type = type;
            return self();
        }

        public SELF type(TypeNodeRequest.Builder inlineType) {
            return this.type(inlineType.build());
        }

        public SELF type(TypeNodeRequest inlineType) {
            this.type = inlineType;
            return self();
        }

        public SELF type(Identified type) {
            type(type.getId());
            return self();
        }

        public SELF file(byte[] fileBytes) {
            this.file = fileBytes;
            return self();
        }

        public SELF stateSource(String stateSource) {
            this.stateSource = stateSource;
            return self();
        }

        public SELF camouflage(Camouflage camouflage) {
            this.camouflage = camouflage;
            return self();
        }

        public SELF camouflage(Camouflage.Builder builder) {
            return camouflage(builder.build());
        }

        public SELF authorizations(List<String> nodeIds) {
            authorizations.addAll(nodeIds);
            return self();
        }

        public SELF authorization(String nodeId) {
            return authorizations(Collections.singletonList(nodeId));
        }

        public SELF authorization(Identified node) {
            return authorization(node.getId());
        }

        public SELF authorizations(String... nodeIds) {
            return authorizations(Arrays.asList(nodeIds));
        }

        public SELF authorizations(Identified... nodes) {
            return authorizations(Arrays.stream(nodes)
                    .map(node -> node.getId())
                    .collect(Collectors.toList()));
        }

        public SELF data(String key, Object value) {
            data.put(key, value);
            return self();
        }

        public SELF data(Object object) {
            ObjectMapper mapper = ObjectMapperFactory.createSlglObjectMapper();
            MapType mapType = mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class);
            Map<String, Object> map = mapper.convertValue(object, mapType);
            return data(map);
        }

        public SELF data(Map<String, Object> data) {
            this.data.putAll(data);
            return self();
        }

        public SELF remove(String key) {
            data.remove(key);
            return self();
        }

        public SELF linkNodes(String anchor, NodeRequest.Builder<?>... nodes) {
            for (Builder<?> node : nodes) {
                linkNode(anchor, node);
            }
            return self();
        }

        public SELF linkNodes(String anchor, NodeRequest... nodes) {
            return linkNodes(anchor, Arrays.asList(nodes));
        }

        public SELF linkNodes(String anchor, Iterable<? extends NodeRequest> nodes) {
            for (NodeRequest node : nodes) {
                linkNode(anchor, node);
            }
            return self();
        }

        public SELF linkNode(String anchor, NodeRequest.Builder<?> node) {
            return linkNode(anchor, node.build());
        }

        public SELF linkNode(String anchor, NodeRequest node) {
            Preconditions.checkArgument(anchor.startsWith("#"));

            @SuppressWarnings("unchecked")
            List<Object> anchorLinks = (List<Object>) data
                    .computeIfAbsent(anchor, it -> new ArrayList<>());

            anchorLinks.add(node);
            return self();
        }

        public SELF linkObservers(ObserverNodeRequest.Builder... observers) {
            return linkNodes("#observers", observers);
        }

        public SELF linkObservers(Iterable<ObserverNodeRequest> observers) {
            return linkNodes("#observers", observers);
        }

        public SELF linkObservers(ObserverNodeRequest... observers) {
            return linkNodes("#observers", observers);
        }

        public SELF linkObserver(ObserverNodeRequest.Builder observer) {
            return linkNode("#observers", observer);
        }

        public SELF linkObserver(ObserverNodeRequest observer) {
            return linkNode("#observers", observer);
        }

        public SELF linkAuditors(AuditorNodeRequest.Builder... auditors) {
            return linkNodes("#auditors", auditors);
        }

        public SELF linkAuditors(Iterable<AuditorNodeRequest> auditors) {
            return linkNodes("#auditors", auditors);
        }

        public SELF linkAuditors(AuditorNodeRequest... auditors) {
            return linkNodes("#auditors", auditors);
        }

        public SELF linkAuditor(AuditorNodeRequest.Builder auditor) {
            return linkNode("#auditors", auditor);
        }

        public SELF linkAuditor(AuditorNodeRequest auditor) {
            return linkNode("#auditors", auditor);
        }

        @SuppressWarnings("unchecked")
        protected SELF self() {
            return (SELF) this;
        }

        protected Object get(String dataKey) {
            return data.get(dataKey);

        }

        @Override
        @JsonValue
        public NodeRequest build() {
            return new NodeRequest(this);
        }
    }
}
