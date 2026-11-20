package com.mlops.modellifecycle.domain;

public class PolicyViolation extends IllegalStateException {
    public PolicyViolation(String message) {
        super(message);
    }
}
