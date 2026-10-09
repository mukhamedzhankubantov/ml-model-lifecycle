package com.mlops.modellifecycle.persistence;

import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.domain.ModelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ModelJdbcTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ModelJdbc models;

    @BeforeEach
    void cleanDatabase() throws IOException {
        jdbc.execute("DROP TABLE IF EXISTS ml_model");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
    }

    @Test
    void insertThenCountIsOne() {
        ModelKey key = new ModelKey("ML-17");

        models.insert(ModelId.newId(), key, ModelStatus.DRAFT, "Churn model");

        assertEquals(1, models.count(key));
    }

    @Test
    void countOfMissingKeyIsZero() {
        assertEquals(0, models.count(new ModelKey("ML-404")));
    }

    @Test
    void statusRoundTrips() {
        ModelKey key = new ModelKey("ML-18");
        models.insert(ModelId.newId(), key, ModelStatus.EVALUATED, "Fraud model");

        assertEquals(ModelStatus.EVALUATED, models.findStatus(key).orElseThrow());
    }

    @Test
    void missingKeyHasNoStatus() {
        assertTrue(models.findStatus(new ModelKey("ML-404")).isEmpty());
    }

    @Test
    void duplicateKeyViolatesUnique() {
        ModelKey key = new ModelKey("ML-17");
        models.insert(ModelId.newId(), key, ModelStatus.DRAFT, "Churn model");

        assertThrows(DataIntegrityViolationException.class,
                () -> models.insert(ModelId.newId(), key, ModelStatus.DRAFT, "Churn again"));
    }

    @Test
    void unknownStatusTextThrows() {
        assertThrows(IllegalStateException.class, () -> ModelJdbc.toStatus("BOGUS"));
    }
}