package com.mlops.modellifecycle.service;

import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelItem;
import com.mlops.modellifecycle.domain.ModelNotFound;
import com.mlops.modellifecycle.domain.ModelStatus;
import com.mlops.modellifecycle.domain.PolicyViolation;
import com.mlops.modellifecycle.persistence.ModelJdbc;
import com.mlops.modellifecycle.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelEvaluationService {
    private static final Logger log = LoggerFactory.getLogger(ModelEvaluationService.class);

    private final ModelJdbc models;
    private final ModelService policyAndRegistry;

    public ModelEvaluationService(ModelJdbc models, ModelService policyAndRegistry) {
        this.models = models;
        this.policyAndRegistry = policyAndRegistry;
    }

    @PreAuthorize("hasAnyRole('" + Roles.SCIENTIST + "','" + Roles.LEAD + "')")
    @Transactional
    public ModelItem evaluate(ModelId id, String actor) {
        ModelItem item = models.findById(id).orElseThrow(() -> new ModelNotFound(id));

        policyAndRegistry.changeModelStatus(id, item.status(), ModelStatus.EVALUATED, 0.0);

        if (!models.updateStatus(id, item.status(), ModelStatus.EVALUATED)) {
            throw new PolicyViolation("model status changed while evaluating, try again");
        }
        log.info("model {} evaluated {} -> EVALUATED by {}", id.getValue(), item.status(), actor);
        return new ModelItem(item.id(), item.key(), ModelStatus.EVALUATED, item.name());
    }
}
