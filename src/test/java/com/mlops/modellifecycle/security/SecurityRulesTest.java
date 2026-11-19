package com.mlops.modellifecycle.security;

import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelItem;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.domain.ModelStatus;
import com.mlops.modellifecycle.handler.MeController;
import com.mlops.modellifecycle.handler.ModelController;
import com.mlops.modellifecycle.service.ModelPromotionService;
import com.mlops.modellifecycle.service.ModelRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One test per row of the README rules table. The real SecurityConfig is imported and the
 * filters are on; only the services and the JwtDecoder are mocks. jwt() supplies a fake
 * verified token with authorities; no jwt() means anonymous.
 */
@WebMvcTest({ModelController.class, MeController.class})
@Import(SecurityConfig.class)
class SecurityRulesTest {

    private static final String MODELS = "/api/v1/models";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ModelRegistrationService registration;

    @MockitoBean
    ModelPromotionService promotion;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private static RequestPostProcessor junior() {
        return jwt().jwt(j -> j.subject("scientist"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + Roles.SCIENTIST));
    }

    private static RequestPostProcessor senior() {
        return jwt().jwt(j -> j.subject("lead"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + Roles.LEAD));
    }

    private static String promotePath() {
        return MODELS + "/" + UUID.randomUUID() + "/promote";
    }

    // ---- No token: 401 -------------------------------------------------------------

    @Test
    void readWithNoTokenIs401AndServiceIsNotCalled() throws Exception {
        mvc.perform(get(MODELS + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("valid token required"));

        verifyNoInteractions(registration);
    }

    @Test
    void createWithNoTokenIs401NotCreated() throws Exception {
        mvc.perform(post(MODELS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"ML-17\",\"name\":\"Churn model\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(registration);
    }

    @Test
    void promoteWithNoTokenIs401AndServiceIsNotCalled() throws Exception {
        mvc.perform(post(promotePath())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accuracy\":0.91}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(promotion);
    }

    @Test
    void meWithNoTokenIs401() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenIs401WithTheSameBody() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("bad signature"));

        mvc.perform(get(MODELS + "/" + UUID.randomUUID())
                        .header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("valid token required"));

        verifyNoInteractions(registration);
    }

    @Test
    void errorBodyEchoesTheCorrelationIdOrMintsOne() throws Exception {
        mvc.perform(get("/api/me").header("X-Correlation-Id", "trace-401"))
                .andExpect(jsonPath("$.correlationId").value("trace-401"));

        mvc.perform(get("/api/me"))
                .andExpect(jsonPath("$.correlationId").value(matchesPattern("[0-9a-f-]{36}")));
    }

    // ---- Locked action: junior 403, senior ok -------------------------------------

    @Test
    void juniorOnPromoteIs403AndServiceIsNotCalled() throws Exception {
        mvc.perform(post(promotePath())
                        .with(junior())
                        .header("X-Correlation-Id", "trace-403")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accuracy\":0.91}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("not allowed for your role"))
                .andExpect(jsonPath("$.correlationId").value("trace-403"));

        verifyNoInteractions(promotion);
    }

    @Test
    void juniorCannotPromoteByPuttingARoleInTheBody() throws Exception {
        mvc.perform(post(promotePath())
                        .with(junior())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accuracy\":0.91,\"role\":\"ML_LEAD\",\"actor\":\"lead\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(promotion);
    }

    @Test
    void seniorOnPromoteSucceedsAndServiceIsCalledWithTheTokenName() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(promotion.promote(any(ModelId.class), anyString(), anyDouble()))
                .thenReturn(new ModelItem(new ModelId(uuid), new ModelKey("ML-17"),
                        ModelStatus.PRODUCTION, "Churn model"));

        mvc.perform(post(MODELS + "/" + uuid + "/promote")
                        .with(senior())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accuracy\":0.91}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRODUCTION"));

        verify(promotion).promote(new ModelId(uuid), "lead", 0.91);
    }

    // ---- Both roles: create, read, me ---------------------------------------------

    @Test
    void juniorMayCreate() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(registration.register(any(ModelId.class), any(ModelKey.class), anyString()))
                .thenReturn(new ModelItem(new ModelId(uuid), new ModelKey("ML-17"),
                        ModelStatus.DRAFT, "Churn model"));

        mvc.perform(post(MODELS)
                        .with(junior())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"ML-17\",\"name\":\"Churn model\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void seniorMayRead() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(registration.get(any(ModelId.class)))
                .thenReturn(new ModelItem(new ModelId(uuid), new ModelKey("ML-17"),
                        ModelStatus.DRAFT, "Churn model"));

        mvc.perform(get(MODELS + "/" + uuid).with(senior()))
                .andExpect(status().isOk());
    }

    @Test
    void meReturnsTheUsernameFromTheToken() throws Exception {
        mvc.perform(get("/api/me").with(junior()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("scientist"));
    }

    // ---- Anything else: denyAll ---------------------------------------------------

    @Test
    void unknownUrlWithATokenIs403() throws Exception {
        mvc.perform(get("/admin").with(senior()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unknownUrlWithNoTokenIs401() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenWithoutAnyRoleIsRefusedOnTheApi() throws Exception {
        mvc.perform(get(MODELS + "/" + UUID.randomUUID())
                        .with(jwt().jwt(j -> j.subject("nobody"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(registration);
    }

    // ---- Open rows: never 401 or 403 ----------------------------------------------

    @Test
    void pageAndSpringdocPathsAreOpenWithNoToken() throws Exception {
        for (String path : new String[]{"/", "/index.html", "/styles.css", "/app.js",
                "/swagger-ui.html", "/swagger-ui/index.html", "/v3/api-docs"}) {
            int code = mvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertNotEquals(401, code, path + " must not ask for a token");
            assertNotEquals(403, code, path + " must not be forbidden");
        }
    }

    @Test
    void openingThePageDoesNotOpenTheApi() throws Exception {
        mvc.perform(get("/api/v1/models/" + UUID.randomUUID() + "/index.html"))
                .andExpect(status().isUnauthorized());
    }
}
