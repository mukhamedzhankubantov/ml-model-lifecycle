package com.mlops.modellifecycle.domain;

public interface ModelRule {
    void check(ModelStatus currentStatus, ModelStatus targetStatus, double accuracy);
}