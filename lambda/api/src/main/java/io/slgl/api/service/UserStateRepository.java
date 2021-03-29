package io.slgl.api.service;

import io.slgl.api.ExecutionContext;
import io.slgl.api.config.Provider;
import io.slgl.api.error.ApiException;
import io.slgl.api.utils.AttributeValues;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.LambdaEnv;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Objects.equal;
import static io.slgl.api.utils.json.UncheckedObjectMapper.MAPPER;
import static java.util.Collections.singletonMap;

@Slf4j
public class UserStateRepository {

    private static final String USER_TABLE = LambdaEnv.getUserDataDynamoDbTable();

    private final Provider<DynamoDbClient> dynamoDB = ExecutionContext.getProvider(DynamoDbClient.class);

    public void createUserEntry(String userId) {
        var request = PutItemRequest.builder()
                .tableName(USER_TABLE)
                .item(Map.of(
                        "id", AttributeValues.s(userId),
                        "credits", AttributeValues.n(0)
                ))
                .build();
        dynamoDB.get().putItem(request);
    }

    public void addKey(String userId, String keyId, String key) {
        addToSet(userId, keyId, key, "secret_keys");
    }

    public void removeKey(String userId, String keyId) {
        removeFromSet(userId, keyId, "secret_keys");
    }

    public Optional<String> getKey(String userId, String keyId) {
        var item = getUserItem(userId);
        var set = item.get("secret_keys");
        if (set == null || !set.hasSs()) {
            return Optional.empty();
        }

        return set.ss().stream()
                .map(KeyRepresentation::fromString)
                .filter(el -> equal(el.id, keyId))
                .map(el -> el.value)
                .findFirst();
    }

    public Optional<String> getFirstKey(String userId) {
        var item = getUserItem(userId);
        var set = item.get("secret_keys");
        if (set == null || !set.hasSs()) {
            return Optional.empty();
        }

        return set.ss().stream()
                .map(KeyRepresentation::fromString)
                .map(el -> el.value)
                .findFirst();
    }


    private Map<String, AttributeValue> getUserItem(String userId) {
        var request = QueryRequest.builder()
                .tableName(USER_TABLE)
                .keyConditionExpression("id = :v_id")
                .expressionAttributeValues(Map.of(
                        ":v_id", AttributeValues.s(userId)
                ))
                .consistentRead(true)
                .build();
        var response = dynamoDB.get().query(request);
        var items = response.items();

        var iterator = items.iterator();
        Map<String, AttributeValue> item = null;
        while (iterator.hasNext()) {
            if (item != null) {
                log.error("It seems that multiple instances of user " + userId + " have been created.");
                throw new ApiException(ErrorCode.UNKNOWN_ERROR);
            }
            item = iterator.next();
        }
        if (item == null) {
            throw new ApiException(ErrorCode.NODE_NOT_FOUND);
        }
        return item;
    }

    private void addToSet(String userId, String elementId, String elementValue, String collectionName) {
        var keyData = new KeyRepresentation(elementId, elementValue, false);
        var request = UpdateItemRequest.builder()
                .tableName(USER_TABLE)
                .key(singletonMap("id", AttributeValues.s(userId)))
                .updateExpression("ADD #collection :to_add")
                .expressionAttributeNames(Map.of(
                        "#collection", collectionName
                ))
                .expressionAttributeValues(Map.of(
                        ":to_add", AttributeValues.ss(keyData.toJson())
                ))
                .build();
        dynamoDB.get().updateItem(request);
    }

    private void removeFromSet(String userId, String elementId, String collectionName) {
        var item = getUserItem(userId);
        var set = item.get(collectionName);
        if (set == null || !set.hasSs()) {
            return;
        }
        Set<String> toRemove = set.ss().stream().filter(el -> {
            var data = KeyRepresentation.fromString(el);
            return data.getId().equals(elementId);
        }).collect(Collectors.toSet());

        if (!toRemove.isEmpty()) {
            var request = UpdateItemRequest.builder()
                    .tableName(USER_TABLE)
                    .key(singletonMap("id", AttributeValues.s(userId)))
                    .updateExpression("DELETE #collection :to_remove")
                    .expressionAttributeNames(Map.of(
                            "#collection", collectionName
                    ))
                    .expressionAttributeValues(Map.of(
                            ":to_remove", AttributeValues.ss(toRemove)
                    ))
                    .build();

            dynamoDB.get().updateItem(request);
        }
    }

    public void addCredits(String userId, BigInteger credits) {
        addToNumber(userId, credits, "credits");
    }

    public BigInteger chargeCredits(String userId, BigInteger credits) {
        return addToNumber(userId, credits.negate(), "credits");
    }

    private BigInteger addToNumber(String userId, BigInteger credits, String columnName) {
        var request = UpdateItemRequest.builder()
                .tableName(USER_TABLE)
                .key(singletonMap("id", AttributeValues.s(userId)))
                .updateExpression("SET #column = if_not_exists(#column, :zero) + :amount_to_add")
                .expressionAttributeNames(Map.of(
                        "#column", columnName
                ))
                .expressionAttributeValues(Map.of(
                        ":amount_to_add", AttributeValues.n(credits),
                        ":zero", AttributeValues.n(0)
                ))
                .returnValues(ReturnValue.UPDATED_NEW)
                .build();
        var result = dynamoDB.get().updateItem(request);
        return Optional.ofNullable(result.attributes().get(columnName))
                .map(AttributeValue::n)
                .map(BigInteger::new)
                .orElseThrow(() -> new IllegalStateException("dynamo should always return new column value"));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class KeyRepresentation {
        private String id;
        private String value;
        private boolean disabled;

        public static KeyRepresentation fromString(String json) {
            return MAPPER.readValue(json, KeyRepresentation.class);
        }

        public String toJson() {
            return MAPPER.writeValueAsString(this);
        }

    }
}
