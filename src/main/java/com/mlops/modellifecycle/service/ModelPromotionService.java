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
public class ModelPromotionService {
    private static final Logger log = LoggerFactory.getLogger(ModelPromotionService.class);

    private final ModelJdbc models;
    private final ModelService policyAndRegistry;

    public ModelPromotionService(ModelJdbc models, ModelService policyAndRegistry) {
        this.models = models;
        this.policyAndRegistry = policyAndRegistry;
    }

    @PreAuthorize("hasRole('" + Roles.LEAD + "')")
    @Transactional
    public ModelItem promote(ModelId id, String actor, double accuracy) {
        ModelItem item = models.findById(id).orElseThrow(() -> new ModelNotFound(id));

        policyAndRegistry.changeModelStatus(id, item.status(), ModelStatus.PRODUCTION, accuracy);

        if (!models.updateStatus(id, item.status(), ModelStatus.PRODUCTION)) {
            throw new PolicyViolation("model status changed while promoting, try again");
        }
        log.info("model {} promoted {} -> PRODUCTION by {}", id.getValue(), item.status(), actor);
        return new ModelItem(item.id(), item.key(), ModelStatus.PRODUCTION, item.name());
    }
}
