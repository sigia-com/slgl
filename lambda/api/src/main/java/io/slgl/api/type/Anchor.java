package io.slgl.api.type;

import io.slgl.api.ExecutionContext;
import io.slgl.api.model.TypeEntity.AnchorEntity;

import java.util.Optional;

public class Anchor {

    private final TypeFactory typeFactory = ExecutionContext.get(TypeFactory.class);

    private final AnchorEntity entity;

    public Anchor(AnchorEntity entity) {
        this.entity = entity;
    }

    public Optional<Type> getType() {
        if (entity.getInlineType() != null) {
            return Optional.of(typeFactory.getInlineType(entity.getInlineType()));
        }

        if (entity.getType() != null) {
            return Optional.of(typeFactory.get(entity.getType()));
        }

        return Optional.empty();
    }

    public String getId() {
        return entity.getId();
    }

    public Integer getMaxSize() {
        return entity.getMaxSize();
    }

    public AnchorEntity getEntity() {
        return entity;
    }

    public boolean hasType() {
        return entity.hasType();
    }
}
