package com.mlops.modellifecycle.service;

import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelPolicy;
import com.mlops.modellifecycle.domain.ModelRegistryPort;
import com.mlops.modellifecycle.domain.ModelStatus;
import org.springframework.stereotype.Service;

@Service
public class ModelService {
    private final ModelPolicy modelPolicy;
    private final ModelRegistryPort registry;

    public ModelService(ModelPolicy modelPolicy, ModelRegistryPort registry) {
        this.modelPolicy = modelPolicy;
        this.registry = registry;
    }

    public void changeModelStatus(ModelId id, ModelStatus currentStatus, ModelStatus targetStatus, double accuracy) {
        modelPolicy.validate(currentStatus, targetStatus, accuracy);
        if (targetStatus == ModelStatus.PRODUCTION) {
            registry.publish(id);
        }
    }
}