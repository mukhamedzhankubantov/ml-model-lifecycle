package com.mlops.modellifecycle.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
class JwtPropertiesTest {

    private static final String LONG_SECRET = "0123456789abcdef0123456789abcdef";

    @Autowired
    JwtProperties jwt;

    @Test
    void devProfileProvidesSecretIssuerAndFifteenMinuteLifetime() {
        assertTrue(jwt.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8).length >= 32);
        assertEquals("ml-model-lifecycle", jwt.issuer());
        assertEquals(Duration.ofMinutes(15), jwt.ttl());
    }

    @Test
    void shortSecretFailsAtStartup() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtProperties("secret", "ml-model-lifecycle", Duration.ofMinutes(15)));
    }

    @Test
    void missingSecretFails() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtProperties(null, "ml-model-lifecycle", Duration.ofMinutes(15)));
    }

    @Test
    void blankIssuerFails() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtProperties(LONG_SECRET, " ", Duration.ofMinutes(15)));
    }

    @Test
    void nonPositiveLifetimeFails() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtProperties(LONG_SECRET, "ml-model-lifecycle", Duration.ZERO));
    }

    @Test
    void thirtyTwoByteSecretIsAccepted() {
        assertDoesNotThrow(
                () -> new JwtProperties(LONG_SECRET, "ml-model-lifecycle", Duration.ofMinutes(15)));
    }
}
