package io.slgl.client.node.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.slgl.client.node.TemplateNodeRequest;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;

import static io.slgl.client.node.permission.InstanceHolder.register;
import static io.slgl.client.node.permission.InstanceHolder.registerNested;

@SuppressWarnings("unused")
public interface Op<VALUE_TYPE> {

    Op<OffsetDateTime> AFTER = register("after", OffsetDateTime.class);
    Op<OffsetDateTime> BEFORE = register("before", OffsetDateTime.class);
    Op<TemplateNodeRequest> MATCHES_TEMPLATE = register("matches_template", TemplateNodeRequest.class);
    Op<Object> EQUAL = register("==", Object.class);
    Op<Object> NOT_EQUAL = register("!=", Object.class);
    Op<Number> LESS_THAN = register("<", Number.class);
    Op<Number> LESS_THAN_OR_EQUAL = register("<=", Number.class);
    Op<Number> GREATER_THAN = register(">", Number.class);
    Op<Number> GREATER_THAN_OR_EQUAL = register(">=", Number.class);
    Op<Object> CONTAINS = register("contains", Object.class);
    Op<Object> DOES_NOT_CONTAIN = register("does_not_contain", Object.class);
    Op<Collection<?>> CONTAINS_ANY_OF = register("contains_any_of", Collection.class);

    NestedRequirementsOp AT_LEAST_ONE_MEETS_REQUIREMENTS = registerNested("at_least_one_meets_requirements", "some");
    NestedRequirementsOp NONE_MEETS_REQUIREMENTS = registerNested("none_meets_requirements", "none");
    NestedRequirementsOp ALL_MEET_REQUIREMENTS = registerNested("all_meet_requirements", "all");

    @JsonValue
    String getOp();

    Class<? super VALUE_TYPE> getType();

    interface NestedRequirementsOp extends Op<Map<String, Requirement.Spec>> {
        String getCollectionOperation();
    }

    @JsonCreator
    static Op<?> byOp(String op) {
        return InstanceHolder.byOp(op);
    }

}
