package com.mlops.modellifecycle.domain;

import java.util.Objects;

public class ModelId {
    private final String value;
    public ModelId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value cannot be null");
        this.value = value;
    }
    public String getValue() {
        return value;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelId modelId = (ModelId) o;
        return Objects.equals(value, modelId.value);
    }
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    @Override
    public String toString() {
        return "ModelId{" + "value='" + value + '\'' + '}';
    }
}
