package com.mlops.modellifecycle.domain;

public class AccuracyStopRule implements ModelRule {
    private final double minAccuracy;
    public AccuracyStopRule(double minAccuracy) {
        this.minAccuracy = minAccuracy;
    }
    @Override
    public void check(ModelStatus currentStatus, ModelStatus targetStatus, double accuracy) {
        if (targetStatus == ModelStatus.PRODUCTION && accuracy < minAccuracy) {
            throw new IllegalArgumentException(
                    "Model accuracy (" + accuracy + ") is below required threshold (" + minAccuracy + ") for PRODUCTION!"
            );
        }
    }
}