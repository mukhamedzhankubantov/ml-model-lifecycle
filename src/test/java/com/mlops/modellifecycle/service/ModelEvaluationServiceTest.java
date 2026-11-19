package com.mlops.modellifecycle.service;

import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelItem;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.domain.ModelNotFound;
import com.mlops.modellifecycle.domain.ModelStatus;
import com.mlops.modellifecycle.domain.PolicyViolation;
import com.mlops.modellifecycle.persistence.ModelJdbc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("dev")
class ModelEvaluationServiceTest {

    @Autowired
    ModelEvaluationService evaluation;

    @Autowired
    ModelJdbc models;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() throws IOException {
        jdbc.execute("DROP TABLE IF EXISTS ml_model");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
    }

    private ModelId insert(String key, ModelStatus status) {
        ModelId id = ModelId.newId();
        models.insert(id, new ModelKey(key), status, "Churn model");
        return id;
    }

    @Test
    @WithMockUser(username = "scientist", roles = "DATA_SCIENTIST")
    void juniorEvaluatesADraftModel() {
        ModelId id = insert("ML-1", ModelStatus.DRAFT);

        ModelItem result = evaluation.evaluate(id, "scientist");

        assertEquals(ModelStatus.EVALUATED, result.status());
        assertEquals(ModelStatus.EVALUATED, models.findById(id).orElseThrow().status());
    }

    @Test
    @WithMockUser(username = "lead", roles = "ML_LEAD")
    void seniorMayEvaluateToo() {
        ModelId id = insert("ML-2", ModelStatus.DRAFT);

        assertEquals(ModelStatus.EVALUATED, evaluation.evaluate(id, "lead").status());
    }

    @Test
    @WithMockUser(username = "lead", roles = "ML_LEAD")
    void evaluatingAnEvaluatedModelIsRefusedAndRowIsUnchanged() {
        ModelId id = insert("ML-3", ModelStatus.EVALUATED);

        assertThrows(PolicyViolation.class, () -> evaluation.evaluate(id, "lead"));

        assertEquals(ModelStatus.EVALUATED, models.findById(id).orElseThrow().status());
    }

    @Test
    @WithMockUser(username = "lead", roles = "ML_LEAD")
    void missingModelIsNotFound() {
        assertThrows(ModelNotFound.class, () -> evaluation.evaluate(ModelId.newId(), "lead"));
    }

    @Test
    void anonymousCallIsRefused() {
        ModelId id = insert("ML-4", ModelStatus.DRAFT);

        assertThrows(AuthenticationException.class, () -> evaluation.evaluate(id, "nobody"));

        assertEquals(ModelStatus.DRAFT, models.findById(id).orElseThrow().status());
    }
}
