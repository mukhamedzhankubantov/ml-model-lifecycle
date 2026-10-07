package com.mlops.modellifecycle.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelPolicyTest {

    private final ModelPolicy policy = new ModelPolicy(List.of(
            new TransitionRule(),
            new AccuracyStopRule(0.80)
    ));

    @ParameterizedTest(name = "{0} -> {1} at {2}, allowed={3}")
    @CsvSource({
            "DRAFT,     EVALUATED,  0.90, true",
            "EVALUATED, PRODUCTION, 0.90, true",
            "DRAFT,     PRODUCTION, 0.90, false",
            "EVALUATED, PRODUCTION, 0.50, false"
    })
    void statusTable(ModelStatus from, ModelStatus to, double accuracy, boolean allowed) {
        if (allowed) {
            assertDoesNotThrow(() -> policy.validate(from, to, accuracy));
        } else {
            assertThrows(IllegalStateException.class, () -> policy.validate(from, to, accuracy));
        }
    }
}