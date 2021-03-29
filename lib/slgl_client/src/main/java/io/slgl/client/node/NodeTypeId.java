package io.slgl.client.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public interface NodeTypeId {

    @JsonCreator
    static SimpleId simple(String type) {
        return new SimpleId(type);
    }

    @JsonCreator
    static ExtendsId extended(@JsonProperty("extends_type") String type) {
        return new ExtendsId(type);
    }


    class SimpleId implements NodeTypeId {
        private final String type;

        private SimpleId(String type) {
            this.type = type;
        }

        @JsonValue
        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            return "SimpleNodeType{" +
                    "type='" + type + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleId that = (SimpleId) o;
            return Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }
    }

    class ExtendsId implements NodeTypeId {
        private final String extendsType;

        private ExtendsId(String extendsType) {
            this.extendsType = extendsType;
        }

        @JsonProperty("extends_type")
        public String getExtendsType() {
            return extendsType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExtendsId that = (ExtendsId) o;
            return Objects.equals(extendsType, that.extendsType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(extendsType);
        }

        @Override
        public String toString() {
            return "ExtendedNodeType{" +
                    "extendsType='" + extendsType + '\'' +
                    '}';
        }
    }
}
