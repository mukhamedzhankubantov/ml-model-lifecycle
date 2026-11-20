package com.mlops.modellifecycle.service;

import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelItem;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.domain.ModelNotFound;
import com.mlops.modellifecycle.domain.ModelRegistryPort;
import com.mlops.modellifecycle.domain.ModelStatus;
import com.mlops.modellifecycle.domain.PolicyViolation;
import com.mlops.modellifecycle.persistence.ModelJdbc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("dev")
class ModelPromotionServiceTest {

    @Autowired
    ModelPromotionService promotion;

    @Autowired
    ModelJdbc models;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    ModelRegistryPort registry;

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

    private ModelStatus statusOf(ModelId id) {
        return models.findById(id).orElseThrow().status();
    }

    @Test
    @WithMockUser(username = "lead", roles = "ML_LEAD")
    void leadPromotesEvaluatedModelAndRowChanges() {
        ModelId id = insert("ML-1", ModelStatus.EVALUATED);

        ModelItem promoted = promotion.promote(id, "lead", 0.91);

        assertEquals(ModelStatus.PRODUCTION, promoted.status());
        assertEquals(ModelStatus.PRODUCTION, statusOf(id));
        verify(registry).publish(id);
    }

    @Test
    @WithMockUser(username = "lead", roles = "ML_LEAD")
    void policyForbidsDraftToProductionAndRowDoesNotChange() {
        ModelId id = insert("ML-2", ModelStatus.DRAFT);

        assertThrows(PolicyViolation.class, () -> promotion.promote(id, "lead", 0.95));

        assertEquals(ModelStatus.DRAFT, statusOf(id));
        verify(registry, never()).publish(id);
    }

    @Test
    @WithMockUser(username = "lead", roles = "ML_LEAD")
    void lowAccuracyIsRefusedAndRowDoesNotChange() {
        ModelId id = insert("ML-3", ModelStatus.EVALUATED);

        assertThrows(PolicyViolation.class, () -> promotion.promote(id, "lead", 0.50));

        assertEquals(ModelStatus.EVALUATED, statusOf(id));
    }

    @Test
    @WithMockUser(username = "lead", roles = "ML_LEAD")
    void missingModelIsNotFound() {
        assertThrows(ModelNotFound.class, () -> promotion.promote(ModelId.newId(), "lead", 0.95));
    }

    @Test
    @WithMockUser(username = "scientist", roles = "DATA_SCIENTIST")
    void juniorRoleIsRefusedByTheMethodRuleAndRowDoesNotChange() {
        ModelId id = insert("ML-4", ModelStatus.EVALUATED);

        assertThrows(AccessDeniedException.class, () -> promotion.promote(id, "scientist", 0.95));

        assertEquals(ModelStatus.EVALUATED, statusOf(id));
        verifyNoInteractions(registry);
    }

    @Test
    void anonymousCallIsRefused() {
        ModelId id = insert("ML-5", ModelStatus.EVALUATED);

        assertThrows(org.springframework.security.core.AuthenticationException.class,
                () -> promotion.promote(id, "nobody", 0.95));

        assertEquals(ModelStatus.EVALUATED, statusOf(id));
    }
}
