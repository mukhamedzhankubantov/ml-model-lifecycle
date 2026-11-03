package com.mlops.modellifecycle.service;

import com.mlops.modellifecycle.domain.DuplicateModel;
import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.persistence.ModelJdbc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ModelRegistrationServiceTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ModelRegistrationService service;

    @Autowired
    ModelJdbc models;

    @BeforeEach
    void cleanDatabase() throws IOException {
        jdbc.execute("DROP TABLE IF EXISTS ml_model");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
    }

    @Test
    void secondStatementRollsBack() {
        ModelKey key = new ModelKey("ML-17");

        assertThrows(DuplicateModel.class, () -> service.insertTwice(key));

        assertEquals(0, models.count(key));
    }

    @Test
    void secondRequestKeepsTheFirst() {
        ModelKey key = new ModelKey("ML-17");
        service.register(ModelId.newId(), key, "Churn model");

        assertThrows(DuplicateModel.class,
                () -> service.register(ModelId.newId(), key, "Churn again"));

        assertEquals(1, models.count(key));
    }
}