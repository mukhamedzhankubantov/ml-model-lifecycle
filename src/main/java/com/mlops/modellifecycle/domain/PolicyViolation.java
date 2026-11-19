package com.mlops.modellifecycle.domain;

/**
 * The policy forbids this status change. It extends IllegalStateException,
 * so earlier code and tests that expect IllegalStateException keep working,
 * while the web layer can map it to its own error code.
 */
public class PolicyViolation extends IllegalStateException {
    public PolicyViolation(String message) {
        super(message);
    }
}
