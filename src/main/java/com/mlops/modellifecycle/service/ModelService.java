package com.mlops.modellifecycle.service;

import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelPolicy;
import com.mlops.modellifecycle.domain.ModelStatus;
import org.springframework.stereotype.Service;

@Service
public class ModelService {
    private final ModelPolicy modelPolicy;
    public ModelService(ModelPolicy modelPolicy) {
        this.modelPolicy = modelPolicy;
    }
    public void changeModelStatus(ModelId id, ModelStatus currentStatus, ModelStatus targetStatus, double accuracy) {
        modelPolicy.validate(currentStatus, targetStatus, accuracy);
    }
}