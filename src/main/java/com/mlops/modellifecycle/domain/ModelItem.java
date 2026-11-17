package com.mlops.modellifecycle.domain;

public record ModelItem(ModelId id, ModelKey key, ModelStatus status, String name) {
}