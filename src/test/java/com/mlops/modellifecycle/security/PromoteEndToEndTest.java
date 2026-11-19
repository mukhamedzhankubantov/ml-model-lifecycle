package com.mlops.modellifecycle.security;

import com.jayway.jsonpath.JsonPath;
import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.domain.ModelStatus;
import com.mlops.modellifecycle.persistence.ModelJdbc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real login, real token, real filter chain, real database. This is the test that proves the
 * roles claim is turned into ROLE_ authorities (the slice tests use jwt(), which skips that).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PromoteEndToEndTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ModelJdbc models;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() throws IOException {
        jdbc.execute("DROP TABLE IF EXISTS ml_model");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
    }

    private String tokenFor(String user, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(user, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private ModelId insert(String key, ModelStatus status) {
        ModelId id = ModelId.newId();
        models.insert(id, new ModelKey(key), status, "Churn model");
        return id;
    }

    private ModelStatus statusOf(ModelId id) {
        return models.findById(id).orElseThrow().status();
    }

    private org.springframework.test.web.servlet.ResultActions promote(ModelId id, String token) throws Exception {
        var request = post("/api/v1/models/" + id.getValue() + "/promote")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accuracy\":0.91}");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request);
    }

    @Test
    void thePageFlowWorksWithRealTokens() throws Exception {
        String scientist = tokenFor("scientist", "scientist-dev");
        String lead = tokenFor("lead", "lead-dev");

        // scientist registers and evaluates (allowed for both roles)
        String created = mvc.perform(post("/api/v1/models")
                        .header("Authorization", "Bearer " + scientist)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"ML-FLOW\",\"name\":\"Flow model\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mvc.perform(post("/api/v1/models/" + id + "/evaluate").header("Authorization", "Bearer " + scientist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EVALUATED"));

        // scientist cannot promote, the row stays EVALUATED
        mvc.perform(post("/api/v1/models/" + id + "/promote")
                        .header("Authorization", "Bearer " + scientist)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accuracy\":0.91}"))
                .andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/models/" + id)
                        .header("Authorization", "Bearer " + scientist))
                .andExpect(jsonPath("$.status").value("EVALUATED"));

        // lead can
        mvc.perform(post("/api/v1/models/" + id + "/promote")
                        .header("Authorization", "Bearer " + lead)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accuracy\":0.91}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRODUCTION"));
    }

    @Test
    void noTokenIs401AndRowIsUnchanged() throws Exception {
        ModelId id = insert("ML-1", ModelStatus.EVALUATED);

        promote(id, null).andExpect(status().isUnauthorized());

        assertEquals(ModelStatus.EVALUATED, statusOf(id));
    }

    @Test
    void scientistGets403AndRowIsUnchanged() throws Exception {
        ModelId id = insert("ML-2", ModelStatus.EVALUATED);

        promote(id, tokenFor("scientist", "scientist-dev"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("not allowed for your role"));

        assertEquals(ModelStatus.EVALUATED, statusOf(id));
    }

    @Test
    void leadPromotesAndTheStatusChanges() throws Exception {
        ModelId id = insert("ML-3", ModelStatus.EVALUATED);

        promote(id, tokenFor("lead", "lead-dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRODUCTION"));

        assertEquals(ModelStatus.PRODUCTION, statusOf(id));
    }

    @Test
    void leadOnADraftModelGetsTheDomainErrorAndRowIsUnchanged() throws Exception {
        ModelId id = insert("ML-4", ModelStatus.DRAFT);

        promote(id, tokenFor("lead", "lead-dev"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POLICY_VIOLATION"));

        assertEquals(ModelStatus.DRAFT, statusOf(id));
    }
}
