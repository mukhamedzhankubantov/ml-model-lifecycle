package com.mlops.modellifecycle.domain;

/** Only a DRAFT model can move to EVALUATED. */
public class EvaluationRule implements ModelRule {
    @Override
    public void check(ModelStatus currentStatus, ModelStatus targetStatus, double accuracy) {
        if (targetStatus == ModelStatus.EVALUATED && currentStatus != ModelStatus.DRAFT) {
            throw new PolicyViolation("Only a DRAFT model can be evaluated, this one is " + currentStatus + "!");
        }
    }
}
