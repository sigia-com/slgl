package io.slgl.client.node.permission;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.slgl.client.node.TemplateNodeRequest;
import io.slgl.client.utils.Preconditions;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Objects.requireNonNull;


public class Requirement {

    private final String path;
    private final Spec requirement;

    Requirement(String path, Spec requirement) {
        this.path = requireNonNull(path);
        this.requirement = requirement;
    }

    public String getPath() {
        return path;
    }

    public Requirement.Spec getRequirement() {
        return requirement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Requirement that = (Requirement) o;
        return Objects.equals(path, that.path) &&
                Objects.equals(requirement, that.requirement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, requirement);
    }

    public static Builder<?> builder() {
        return new Builder<>();
    }

    public static PathSpec requireThat(String path) {
        return new PathSpec(path);
    }

    public static class Builder<TYPE> {

        private String path;
        private Op<TYPE> op;
        private TYPE value;
        private String var;
        private Aggregate<? extends TYPE> aggregate;

        public <T extends TYPE> Builder<T> op(Op<T> op) {
            @SuppressWarnings("unchecked") Builder<T> self = (Builder<T>) this;
            self.op = op;
            return self;
        }

        public Builder<TYPE> aggregate(Aggregate<? extends TYPE> aggregate) {
            this.aggregate = Aggregate.noAggregate().equals(aggregate) ? null : aggregate;
            return this;
        }

        public Builder<TYPE> value(TYPE value) {
            this.value = value;
            return this;
        }

        public Builder<TYPE> path(String path) {
            this.path = path;
            return this;
        }

        public Builder<TYPE> var(String var) {
            this.var = var;
            return this;
        }

        @JsonValue
        public Requirement build() {
            Spec spec = new Spec(op, aggregate, value, var, null);
            return new Requirement(path, spec);
        }
    }

    public static class PathSpec {
        private final String path;

        public PathSpec(String path) {
            this.path = requireNonNull(path);
        }

        public ValueSpecifier<TemplateNodeRequest> matchesTemplate() {
            return new ValueSpecifier<>(path, Op.MATCHES_TEMPLATE, null);
        }

        public ValueSpecifier<OffsetDateTime> isAfter() {
            return new ValueSpecifier<>(path, Op.AFTER, null);
        }

        public ValueSpecifier<OffsetDateTime> isBefore() {
            return new ValueSpecifier<>(path, Op.BEFORE, null);
        }

        public ValueSpecifier<Object> isEqualTo() {
            return new ValueSpecifier<>(path, Op.EQUAL, null);
        }

        public ValueSpecifier<Object> isNotEqualTo() {
            return new ValueSpecifier<>(path, Op.NOT_EQUAL, null);
        }

        public ValueSpecifier<Number> isLessThan() {
            return new ValueSpecifier<>(path, Op.LESS_THAN, null);
        }

        public ValueSpecifier<Number> isLessThanOrEqualTo() {
            return new ValueSpecifier<>(path, Op.LESS_THAN_OR_EQUAL, null);
        }

        public ValueSpecifier<Number> isGreaterThan() {
            return new ValueSpecifier<>(path, Op.GREATER_THAN, null);
        }

        public ValueSpecifier<Number> isGreaterThanOrEqualTo() {
            return new ValueSpecifier<>(path, Op.GREATER_THAN_OR_EQUAL, null);
        }

        public ValueSpecifier<Object> contains() {
            return new ValueSpecifier<>(path, Op.CONTAINS, null);
        }

        public ValueSpecifier<Object> doesNotContain() {
            return new ValueSpecifier<>(path, Op.DOES_NOT_CONTAIN, null);
        }

        public ValueSpecifier<Collection<Object>> containsAny() {
            return new ValueSpecifier<>(path, Op.CONTAINS, null);
        }

        public NestedRequirementSpecifier atLeastOne() {
            return new NestedRequirementSpecifier(path, Op.AT_LEAST_ONE_MEETS_REQUIREMENTS, null);
        }

        public NestedRequirementSpecifier all() {
            return new NestedRequirementSpecifier(path, Op.ALL_MEET_REQUIREMENTS, null);
        }

        public NestedRequirementSpecifier none() {
            return new NestedRequirementSpecifier(path, Op.NONE_MEETS_REQUIREMENTS, null);
        }

        public NumberPathSpec sum() {
            return new NumberPathSpec(path, Aggregate.SUM);
        }

        public <T> SingleValuePathSpec<T> aggregatedWith(Aggregate<T> aggregate) {
            return new SingleValuePathSpec<>(path, aggregate);
        }
    }

    public static class ValueSpecifier<TYPE> {
        private final Op<? super TYPE> op;
        private final Aggregate<? extends TYPE> aggregate;
        private final String path;

        public ValueSpecifier(String path, Op<? super TYPE> op, Aggregate<? extends TYPE> aggregate) {
            this.op = op;
            this.aggregate = aggregate;
            this.path = path;
        }

        public Requirement value(TYPE value) {
            Spec spec = new Spec(op, aggregate, value, null, null);
            return new Requirement(path, spec);
        }

        public Requirement ref(String var) {
            Spec spec = new Spec(op, aggregate, null, var, null);
            return new Requirement(path, spec);
        }
    }

    public static class NestedRequirementSpecifier {
        private final String path;
        private final String as;
        private Op.NestedRequirementsOp op;

        public NestedRequirementSpecifier(String path, Op.NestedRequirementsOp op, String as) {
            this.path = requireNonNull(path);
            this.as = as;
            this.op = op;
        }

        public NestedRequirementSpecifier as(String alias) {
            return new NestedRequirementSpecifier(path, op, alias);
        }

        public Requirement meetsAllRequirements(Requirement... requirements) {
            return meetsAllRequirements(Requirements.of(requirements));
        }

        public Requirement meetsAllRequirements(Iterable<Requirement> requirements) {
            return meetsAllRequirements(Requirements.of(requirements));
        }

        public Requirement meetsAllRequirements(Requirements.Builder requirements) {
            return meetsAllRequirements(requirements.build());
        }

        public Requirement meetsAllRequirements(Requirements requirements) {
            Spec spec = new Spec(op, null, requirements, null, as);
            return new Requirement(path, spec);
        }

    }

    public static class SingleValuePathSpec<TYPE> {
        protected final Aggregate<? extends TYPE> aggregate;
        private final String path;

        private SingleValuePathSpec(String path, Aggregate<? extends TYPE> aggregate) {
            this.aggregate = aggregate;
            this.path = path;
        }

        public ValueSpecifier<TYPE> equalTo() {
            return new ValueSpecifier<>(path, Op.EQUAL, aggregate);
        }

        public ValueSpecifier<TYPE> notEqualTo() {
            return new ValueSpecifier<>(path, Op.NOT_EQUAL, aggregate);
        }
    }

    public static class NumberPathSpec extends SingleValuePathSpec<Number> {
        private final String path;

        private NumberPathSpec(String path, Aggregate<? extends Number> aggregate) {
            super(path, aggregate);
            this.path = path;
        }

        public ValueSpecifier<Number> isLessThan() {
            return new ValueSpecifier<>(path, Op.LESS_THAN, aggregate);
        }

        public ValueSpecifier<Number> isLessThanOrEqualTo() {
            return new ValueSpecifier<>(path, Op.LESS_THAN_OR_EQUAL, aggregate);
        }

        public ValueSpecifier<Number> isGreaterThan() {
            return new ValueSpecifier<>(path, Op.GREATER_THAN, aggregate);
        }

        public ValueSpecifier<Number> isGreaterThanOrEqualTo() {
            return new ValueSpecifier<>(path, Op.GREATER_THAN_OR_EQUAL, aggregate);
        }
    }


    @JsonInclude(NON_NULL)
    public static class Spec {

        @JsonProperty("aggregate")
        private final Aggregate<?> aggregate;

        @JsonProperty("op")
        private final Op<?> op;

        @JsonProperty("value")
        @JsonSerialize(using = RequirementValueSerializer.class)
        private final Object value;

        @JsonProperty("var")
        private final String var;

        @JsonProperty("as")
        private final String as;

        @JsonCreator
        public Spec(
                @JsonProperty("op") Op<?> op,
                @JsonProperty("aggregate") Aggregate<?> aggregate,
                @JsonProperty("value") Object value,
                @JsonProperty("var") String var,
                @JsonProperty("as") String as
        ) {
            Preconditions.checkState(var == null || value == null, "you cannot set var and value at the same time");

            this.aggregate = aggregate == null || Aggregate.noAggregate().equals(aggregate) ? null : aggregate;
            this.op = op != null ? op : Op.EQUAL;
            this.value = value;
            this.var = var;
            this.as = as;
        }

        public Aggregate<?> getAggregate() {
            return aggregate;
        }

        public Op<?> getOp() {
            return op;
        }

        public Object getValue() {
            return value;
        }

        public String getVar() {
            return var;
        }

        public String getAs() {
            return as;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Spec spec = (Spec) o;
            return Objects.equals(aggregate, spec.aggregate) &&
                    Objects.equals(op, spec.op) &&
                    Objects.equals(value, spec.value) &&
                    Objects.equals(var, spec.var) &&
                    Objects.equals(as, spec.as);
        }

        @Override
        public int hashCode() {
            return Objects.hash(aggregate, op, value, var, as);
        }

        public static <T> Builder<T> builder() {
            return new Builder<>();
        }

        public static class Builder<TYPE> {
            private Op<TYPE> op;
            private TYPE value;
            private String var;
            private String as;
            private Aggregate<? extends TYPE> aggregate;

            public <T extends TYPE> Builder<T> op(Op<T> op) {
                @SuppressWarnings("unchecked") Builder<T> self = (Builder<T>) this;
                self.op = op;
                self.as = null;
                return self;
            }

            public Builder<TYPE> aggregate(Aggregate<? extends TYPE> aggregate) {
                this.aggregate = Aggregate.noAggregate().equals(aggregate) ? null : aggregate;
                return this;
            }

            public Builder<TYPE> value(TYPE value) {
                this.value = value;
                return this;
            }

            public Builder<TYPE> var(String var) {
                this.var = var;
                return this;
            }


            public Builder<TYPE> as(String as) {
                this.as = as;
                return this;
            }

            @JsonValue
            public Spec build() {
                Preconditions.checkState(aggregate == null || op == null || op.getType().isAssignableFrom(aggregate.getType()),
                        "incompatible aggregate and op types");
                Preconditions.checkState(op == null || value == null || op.getType().isInstance(value),
                        "incompatible op and value types");
                Preconditions.checkState(as == null || op instanceof Op.NestedRequirementsOp,
                        "incompatible op and value types");
                return new Spec(
                        op,
                        aggregate,
                        value,
                        var,
                        as
                );
            }

        }
    }

}
