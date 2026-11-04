package com.mlops.modellifecycle.domain;

public class ModelNotFound extends RuntimeException {
    public ModelNotFound(ModelId id) {
        super("model not found: " + id.getValue());
    }
}