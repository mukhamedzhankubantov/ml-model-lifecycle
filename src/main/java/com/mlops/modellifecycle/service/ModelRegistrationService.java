package com.mlops.modellifecycle.service;

import com.mlops.modellifecycle.domain.DuplicateModel;
import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.domain.ModelStatus;
import com.mlops.modellifecycle.persistence.ModelJdbc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelRegistrationService {
    private final ModelJdbc models;

    public ModelRegistrationService(ModelJdbc models) {
        this.models = models;
    }

    @Transactional
    public void register(ModelId id, ModelKey key, String name) {
        try {
            models.insert(id, key, ModelStatus.DRAFT, name);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateModel(key.getValue()); // unchecked -> rollback
        }
    }

    @Transactional
    public void insertTwice(ModelKey key) {
        models.insert(ModelId.newId(), key, ModelStatus.DRAFT, "Churn model");
        try {
            models.insert(ModelId.newId(), key, ModelStatus.DRAFT, "Churn model again");
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateModel(key.getValue()); // unchecked -> rollback
        }
    }
}