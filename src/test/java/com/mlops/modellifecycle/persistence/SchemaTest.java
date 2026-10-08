package com.mlops.modellifecycle.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class SchemaTest {

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() throws IOException {
        jdbc.execute("DROP TABLE IF EXISTS ml_model");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
    }

    @Test
    void schemaBuildsEmptyTable() {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM ml_model", Integer.class);
        assertEquals(0, n);
    }

    @Test
    void unknownStatusIsRejectedByCheck() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update(
                        "INSERT INTO ml_model (id, business_key, status, name) VALUES (?, ?, ?, ?)",
                        UUID.randomUUID(), "ML-99", "BOGUS", "Test"));
    }
}