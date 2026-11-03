package com.mlops.modellifecycle.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelKeyTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void nullOrBlankThrows(String text) {
        assertThrows(IllegalArgumentException.class, () -> new ModelKey(text));
    }

    @Test
    void trimsWhitespace() {
        assertEquals("ML-17", new ModelKey("  ML-17  ").getValue());
    }

    @Test
    void duplicateModelIsUnchecked() {
        RuntimeException ex = new DuplicateModel("ML-17");
        assertEquals("duplicate model: ML-17", ex.getMessage());
    }
}