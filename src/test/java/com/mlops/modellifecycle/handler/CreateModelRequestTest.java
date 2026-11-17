package com.mlops.modellifecycle.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateModelRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> messages(CreateModelRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void blankNameIsRejected(String name) {
        assertEquals(Set.of("name must not be blank"),
                messages(new CreateModelRequest("ML-1", name)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void blankBusinessKeyIsRejected(String key) {
        assertEquals(Set.of("business key must not be blank"),
                messages(new CreateModelRequest(key, "Churn model")));
    }

    @Test
    void nameOf121CharactersIsRejected() {
        assertEquals(Set.of("name size must be between 0 and 120"),
                messages(new CreateModelRequest("ML-1", "x".repeat(121))));
    }

    @Test
    void nameOf120CharactersIsAccepted() {
        assertTrue(messages(new CreateModelRequest("ML-1", "x".repeat(120))).isEmpty());
    }
}