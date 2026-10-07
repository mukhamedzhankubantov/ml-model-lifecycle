package com.mlops.modellifecycle.domain;

public class TransitionRule implements ModelRule {
    @Override
    public void check(ModelStatus currentStatus, ModelStatus targetStatus, double accuracy) {
        if (currentStatus == ModelStatus.DRAFT && targetStatus == ModelStatus.PRODUCTION) {
            throw new IllegalArgumentException("The DRAFT model must first pass through the EVALUATED stage!");
        }
    }
}