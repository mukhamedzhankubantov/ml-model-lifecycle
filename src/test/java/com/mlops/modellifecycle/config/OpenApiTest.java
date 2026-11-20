package com.mlops.modellifecycle.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class OpenApiTest {

    private static final String LOGIN = "$.paths['/api/v1/auth/login'].post.responses";
    private static final String PROMOTE = "$.paths['/api/v1/models/{id}/promote'].post.responses";
    private static final String ME = "$.paths['/api/me'].get.responses";

    @Autowired
    MockMvc mvc;

    @Test
    void apiDocsOpenWithNoToken() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void swaggerUiPageIsNotBehindAToken() throws Exception {
        int code = mvc.perform(get("/swagger-ui.html")).andReturn().getResponse().getStatus();
        assertNotEquals(401, code);
        assertNotEquals(403, code);
    }

    @Test
    void loginNamesItsStatuses() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(LOGIN + ".200").exists())
                .andExpect(jsonPath(LOGIN + ".400").exists())
                .andExpect(jsonPath(LOGIN + ".401").exists());
    }

    @Test
    void promoteNamesSuccessPlus401And403() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(PROMOTE + ".200").exists())
                .andExpect(jsonPath(PROMOTE + ".401").exists())
                .andExpect(jsonPath(PROMOTE + ".403").exists())
                .andExpect(jsonPath(PROMOTE + ".409").exists());
    }

    @Test
    void meNamesItsStatuses() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(ME + ".200").exists())
                .andExpect(jsonPath(ME + ".401").exists());
    }

    @Test
    void protectedOperationsAreMarkedWithTheBearerScheme() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/me'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/models/{id}/promote'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").doesNotExist());
    }

    @Test
    void theAuthenticationArgumentIsNotAnInputOfPromote() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/models/{id}/promote'].post.parameters.length()").value(1))
                .andExpect(jsonPath("$.paths['/api/v1/models/{id}/promote'].post.parameters[0].name").value("id"));
    }

    @Test
    void promoteBodyHasAccuracyAndNoActorOrRole() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.PromoteRequest.properties.accuracy").exists())
                .andExpect(jsonPath("$.components.schemas.PromoteRequest.properties.actor").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.PromoteRequest.properties.role").doesNotExist());
    }
}
