package com.mlops.modellifecycle.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelIdTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void nullOrBlankThrows(String text) {
        assertThrows(IllegalArgumentException.class, () -> ModelId.of(text));
    }

    @Test
    void nullUuidThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ModelId(null));
    }

    @Test
    void ofParsesUuid() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, ModelId.of(uuid.toString()).getValue());
    }

    @Test
    void newIdIsUnique() {
        assertNotEquals(ModelId.newId(), ModelId.newId());
    }
}