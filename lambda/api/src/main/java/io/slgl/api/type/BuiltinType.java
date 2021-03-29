package io.slgl.api.type;

import io.slgl.api.authorization.model.AuthorizationEntity;
import io.slgl.api.model.AuditorEntity;
import io.slgl.api.model.TemplateEntity;
import io.slgl.api.model.TypeEntity;
import io.slgl.api.observer.model.ObserverEntity;
import io.slgl.api.type.model.CreditsEntity;
import io.slgl.api.type.model.DeletionEntity;
import io.slgl.api.type.model.KeyEntity;
import io.slgl.api.type.model.UserEntity;
import io.slgl.api.type.model.value.BooleanEntity;
import io.slgl.api.type.model.value.DateTimeEntity;
import io.slgl.api.type.model.value.NumberEntity;
import io.slgl.api.type.model.value.TextEntity;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static com.google.common.base.Objects.equal;
import static com.google.common.base.Preconditions.checkNotNull;

@Getter
public enum BuiltinType {

    BASE("2c712a34-6501-4d1e-946e-451d4d70328b", "base"),
    TYPE("ce6b07da-96d5-4c09-8659-e9e806dfe1c1", "type", TypeEntity.class),
    PUBLIC_TYPE("7858292f-41ab-49a1-a0a0-49f0fb0ad246" , "public_type"),

    USER("313aa25f-65db-45e9-bb52-74f71ee13ff2", "user", UserEntity.class),
    DELETION("1446deae-13fb-4fe9-a751-48d0d3ebdfa7", "deletion", DeletionEntity.class),
    KEY("3588ea96-6a11-4e76-aa89-3da79a5298e5", "key", KeyEntity.class),
    CREDITS("af7f8403-b442-412a-8891-b0bb6901ed4e", "credits", CreditsEntity.class),

    OBSERVER("01c3d684-0139-4721-bf7f-d2e93b767dda", "observer", ObserverEntity.class),
    TEMPLATE("1507707c-e5d9-4915-85a1-a48e5200323a", "template", TemplateEntity.class),
    AUDITOR("78f4b8d1-0410-48af-a673-7027b21c5d3f", "auditor", AuditorEntity.class),
    AUTHORIZATION("de1c8465-42eb-4c1a-bedc-cbe9ef94dab2", "authorization", AuthorizationEntity.class),
    CAMOUFLAGE("b0728036-c773-4ce7-ae04-6eef66afb3f2", null),
    FAKE("65b01b83-cc1c-4886-b6f7-d516400db391", "fake"),

    TEXT("ef2b332a-8310-4477-8788-2b265b02dc74", "value/text", TextEntity.class),
    NUMBER("f9a449b9-dc60-4ded-b19f-ee63fcb6a803", "value/number", NumberEntity.class),
    BOOLEAN("fc5c2422-e339-4180-b200-d7dccbcf824d", "value/boolean", BooleanEntity.class),
    DATE_TIME("d67e7385-8050-49c3-ac52-0ca9cb395cae", "value/date_time", DateTimeEntity.class),
    ;

    private final String id;
    private final Class<?> entityClass;

    private final String resource;
    private Type type;

    BuiltinType(String id, String resource) {
        this(id, resource, null);
    }

    BuiltinType(String id, String resource, Class<?> entityClass) {
        this.id = id;
        this.resource = resource;
        this.entityClass = entityClass;
    }

    public static Optional<BuiltinType> findById(String id) {
        for (BuiltinType builtinType : values()) {
            if (equal(builtinType.getId(), id)) {
                return Optional.of(builtinType);
            }
        }

        return Optional.empty();
    }

    public static boolean isBuiltinTypeId(String id) {
        return findById(id).isPresent();
    }

    public static void loadTypes() {
        for (BuiltinType builtinType : values()) {
            builtinType.loadType();
        }
    }

    private void loadType() {
        if (type != null || resource == null) {
            return;
        }

        String resourcePath = "/types/" + resource + ".json";

        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            checkNotNull(stream, "Builtin type resource not found: " + resourcePath);

            BuiltinTypeEntity entity = UncheckedObjectMapper.MAPPER.readValue(stream, BuiltinTypeEntity.class);
            entity.setId(id);

            type = new Type(entity.getId(), entity);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
