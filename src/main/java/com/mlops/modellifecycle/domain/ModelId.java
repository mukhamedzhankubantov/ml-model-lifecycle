package com.mlops.modellifecycle.domain;

import java.util.Objects;
import java.util.UUID;

public final class ModelId {
    private final UUID value;

    public ModelId(UUID value) {
        if (value == null) throw new IllegalArgumentException("ModelId value cannot be null");
        this.value = value;
    }

    public static ModelId newId() {
        return new ModelId(UUID.randomUUID());
    }

    public static ModelId of(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("ModelId cannot be null or blank");
        return new ModelId(UUID.fromString(text.trim()));
    }

    public UUID getValue() {
        return value;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return value.equals(((ModelId) o).value);
    }
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    @Override
    public String toString() {
        return "ModelId{value=" + value + "}";
    }
}