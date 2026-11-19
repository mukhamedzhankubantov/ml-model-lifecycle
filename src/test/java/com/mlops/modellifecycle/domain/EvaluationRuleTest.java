package com.mlops.modellifecycle.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvaluationRuleTest {

    private final ModelPolicy policy = new ModelPolicy(List.of(
            new TransitionRule(),
            new EvaluationRule(),
            new AccuracyStopRule(0.80)
    ));

    @ParameterizedTest(name = "{0} -> EVALUATED allowed={1}")
    @CsvSource({
            "DRAFT,      true",
            "EVALUATED,  false",
            "PRODUCTION, false",
            "REJECTED,   false"
    })
    void onlyDraftCanBeEvaluated(ModelStatus from, boolean allowed) {
        if (allowed) {
            assertDoesNotThrow(() -> policy.validate(from, ModelStatus.EVALUATED, 0.0));
        } else {
            assertThrows(PolicyViolation.class, () -> policy.validate(from, ModelStatus.EVALUATED, 0.0));
        }
    }

    @ParameterizedTest(name = "{0} -> PRODUCTION is not affected by the evaluation rule")
    @CsvSource({"EVALUATED", "PRODUCTION"})
    void otherTargetsAreNotTouched(ModelStatus from) {
        assertDoesNotThrow(() -> new EvaluationRule().check(from, ModelStatus.PRODUCTION, 0.9));
    }
}
