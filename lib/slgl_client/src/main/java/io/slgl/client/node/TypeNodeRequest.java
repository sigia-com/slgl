package io.slgl.client.node;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.slgl.client.Types;
import io.slgl.client.node.permission.Permission;
import io.slgl.client.protocol.Identified;

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;


public final class TypeNodeRequest extends NodeRequest {

    private TypeNodeRequest(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends NodeRequest.Builder<Builder> {
        private List<TypeAnchor> anchors;
        private List<String> stateProperties;
        private List<Permission> permissions;

        {
            super.type(Types.TYPE);
        }

        public Builder publicType() {
            type(Types.PUBLIC_TYPE);
            return this;
        }

        public Builder extendsType(Identified extended) {
            data("extends_type", extended.getId());
            return this;
        }

        public Builder extendsType(String typeId) {
            data("extends_type", typeId);
            return this;
        }

        public Builder setAnchors(List<TypeAnchor> anchors) {
            anchors().clear();
            anchors().addAll(anchors);
            return this;
        }

        public Builder setStateProperties(List<String> stateProperties) {
            initializeStateProperties();
            this.stateProperties.clear();
            this.stateProperties.addAll(stateProperties);
            return this;
        }

        public Builder anchor(String id) {
            return addAnchor(new TypeAnchor(id, null, null));
        }

        public Builder anchor(String id, Identified type) {
            return anchor(id, type.getId());
        }

        public Builder anchor(String id, String typeId) {
            return addAnchor(new TypeAnchor(id, typeId, null));
        }

        public Builder anchor(String id, TypeNodeRequest.Builder inlineType) {
            return anchor(id, inlineType.build());
        }

        public Builder anchor(String id, TypeNodeRequest inlineType) {
            return addAnchor(new TypeAnchor(id, inlineType, null));
        }

        public Builder anchor(String id, int maxSize) {
            return addAnchor(new TypeAnchor(id, null, maxSize));
        }

        public Builder anchor(String id, int maxSize, Identified typeId) {
            return anchor(id, maxSize, typeId.getId());
        }

        public Builder anchor(String id, int maxSize, String typeId) {
            return addAnchor(new TypeAnchor(id, typeId, maxSize));
        }

        public Builder anchor(String id, int maxSize, TypeNodeRequest.Builder inlineType) {
            return anchor(id, maxSize, inlineType.build());
        }

        public Builder anchor(String id, int maxSize, TypeNodeRequest inlineType) {
            return addAnchor(new TypeAnchor(id, inlineType, maxSize));
        }

        private Builder addAnchor(TypeAnchor e) {
            anchors().add(e);
            return this;
        }

        private List<TypeAnchor> anchors() {
            if (anchors == null) {
                anchors = new ArrayList<>();
                data("anchors", this.anchors);
            }
            return anchors;
        }

        public Builder permissions(Permission.Builder... permissions) {
            for (Permission.Builder permission : permissions) {
                permission(permission);
            }
            return this;
        }

        public Builder permissions(Iterable<Permission> permissions) {
            for (Permission permission : permissions) {
                permission(permission);
            }
            return this;
        }

        public Builder permissions(Permission... permissions) {
            for (Permission permission : permissions) {
                permission(permission);
            }
            return this;
        }

        public Builder permission(Permission.Builder permission) {
            permissions().add(permission.build());
            return this;
        }

        public Builder permission(Permission permission) {
            if (permission != null) {
                permissions().add(permission);
            }
            return this;
        }

        private List<Permission> permissions() {
            if (permissions == null) {
                permissions = new ArrayList<>();
                data("permissions", permissions);
            }
            return permissions;
        }

        public Builder stateProperties(String... stateProperties) {
            initializeStateProperties();
            this.stateProperties.addAll(Arrays.asList(stateProperties));
            return this;
        }

        public Builder stateProperties(Collection<String> stateProperties) {
            initializeStateProperties();
            this.stateProperties.addAll(stateProperties);
            return this;
        }

        private void initializeStateProperties() {
            if (this.stateProperties == null) {
                this.stateProperties = new ArrayList<>();
                data("state_properties", this.stateProperties);
            }
        }

        public Builder linkTemplates(TemplateNodeRequest.Builder... templates) {
            return linkNodes("#templates", templates);
        }

        public Builder linkTemplates(TemplateNodeRequest... templates) {
            return linkNodes("#templates", templates);
        }

        public Builder linkTemplates(Collection<TemplateNodeRequest> templates) {
            return linkNodes("#templates", templates);
        }

        public Builder linkTemplate(TemplateNodeRequest.Builder template) {
            return linkNode("#templates", template);
        }

        public Builder linkTemplate(TemplateNodeRequest template) {
            return linkNode("#templates", template);
        }

        @Override
        public TypeNodeRequest build() {
            return new TypeNodeRequest(this);
        }
    }

    @JsonInclude(NON_NULL)
    public static class TypeAnchor {

        @JsonProperty("id")
        private final String id;

        @JsonProperty("type")
        private final Object type;

        @JsonProperty("max_size")
        private final Integer maxSize;

        public TypeAnchor(String id, Object type, Integer maxSize) {
            Objects.requireNonNull(id, "id");
            this.id = id;
            this.type = type;
            this.maxSize = maxSize;
        }
    }
}
