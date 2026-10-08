package com.mlops.modellifecycle.domain;

import java.util.Objects;

public final class ModelKey {
    private final String value;

    public ModelKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ModelKey cannot be null or blank");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return value.equals(((ModelKey) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ModelKey{value=" + value + "}";
    }
}