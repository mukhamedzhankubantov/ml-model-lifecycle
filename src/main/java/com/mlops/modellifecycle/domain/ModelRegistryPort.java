package com.mlops.modellifecycle.domain;

public interface ModelRegistryPort {
    void publish(ModelId id);
}