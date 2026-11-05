package com.mlops.modellifecycle.handler;

import com.mlops.modellifecycle.domain.ModelItem;

import java.util.UUID;

public record ModelResponse(UUID id, String businessKey, String status, String name) {

    public static ModelResponse from(ModelItem item) {
        return new ModelResponse(
                item.id().getValue(),
                item.key().getValue(),
                item.status().name(),
                item.name());
    }
}