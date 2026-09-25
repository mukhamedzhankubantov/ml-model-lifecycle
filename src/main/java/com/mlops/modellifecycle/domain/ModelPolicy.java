package com.mlops.modellifecycle.domain;

import java.util.List;

public class ModelPolicy {
    private final List<ModelRule> rules;
    public ModelPolicy(List<ModelRule> rules) {
        this.rules = rules;
    }
    public void validate(ModelStatus currentStatus, ModelStatus targetStatus, double accuracy) {
        for (ModelRule rule : rules) {
            rule.check(currentStatus, targetStatus, accuracy);
        }
    }
}