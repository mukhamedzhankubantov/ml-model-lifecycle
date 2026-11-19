package com.mlops.modellifecycle.handler;

import com.mlops.modellifecycle.domain.DuplicateModel;
import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelItem;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.domain.ModelNotFound;
import com.mlops.modellifecycle.domain.ModelStatus;
import com.mlops.modellifecycle.domain.PolicyViolation;
import com.mlops.modellifecycle.security.Roles;
import com.mlops.modellifecycle.security.SecurityConfig;
import com.mlops.modellifecycle.service.ModelPromotionService;
import com.mlops.modellifecycle.service.ModelRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModelController.class)
@Import(SecurityConfig.class)
class ModelControllerTest {

    private static final String BASE = "/api/v1/models";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ModelRegistrationService service;

    @MockitoBean
    ModelPromotionService promotion;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private static RequestPostProcessor lead() {
        return jwt().jwt(j -> j.subject("lead"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + Roles.LEAD));
    }

    private static RequestPostProcessor scientist() {
        return jwt().jwt(j -> j.subject("scientist"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + Roles.SCIENTIST));
    }

    @Test
    void noTokenReturns401AndServiceIsNotCalled() throws Exception {
        mvc.perform(get(BASE + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("valid token required"));

        verifyNoInteractions(service);
    }

    @Test
    void validBodyReturns201WithLocation() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(service.register(any(ModelId.class), any(ModelKey.class), eq("Churn model")))
                .thenReturn(new ModelItem(new ModelId(uuid), new ModelKey("ML-17"),
                        ModelStatus.DRAFT, "Churn model"));

        mvc.perform(post(BASE)
                        .with(scientist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"ML-17\",\"name\":\"Churn model\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/" + uuid)))
                .andExpect(jsonPath("$.id").value(uuid.toString()))
                .andExpect(jsonPath("$.businessKey").value("ML-17"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.name").value("Churn model"));
    }

    @Test
    void blankNameReturns400AndServiceIsNotCalled() throws Exception {
        mvc.perform(post(BASE)
                        .with(scientist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"ML-17\",\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value(containsString("name")));

        verifyNoInteractions(service);
    }

    @Test
    void missingItemReturns404() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(service.get(any(ModelId.class))).thenThrow(new ModelNotFound(new ModelId(uuid)));

        mvc.perform(get(BASE + "/" + uuid).with(scientist()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void duplicateKeyReturns409() throws Exception {
        when(service.register(any(ModelId.class), any(ModelKey.class), any(String.class)))
                .thenThrow(new DuplicateModel("ML-17"));

        mvc.perform(post(BASE)
                        .with(scientist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"ML-17\",\"name\":\"Churn again\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void businessKeyInPathReturns400() throws Exception {
        mvc.perform(get(BASE + "/ML-17").with(scientist()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("id must be a UUID"));

        verifyNoInteractions(service);
    }

    @Test
    void promoteUsesTheNameFromTheTokenAndIgnoresAnActorInTheBody() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(promotion.promote(any(ModelId.class), any(String.class), anyDouble()))
                .thenReturn(new ModelItem(new ModelId(uuid), new ModelKey("ML-17"),
                        ModelStatus.PRODUCTION, "Churn model"));

        mvc.perform(post(BASE + "/" + uuid + "/promote")
                        .with(lead())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accuracy\":0.91,\"actor\":\"mallory\",\"role\":\"ML_LEAD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRODUCTION"));

        verify(promotion).promote(new ModelId(uuid), "lead", 0.91);
    }

    @Test
    void promoteRefusedByPolicyReturns409() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(promotion.promote(any(ModelId.class), any(String.class), anyDouble()))
                .thenThrow(new PolicyViolation("The DRAFT model must first pass through the EVALUATED stage!"));

        mvc.perform(post(BASE + "/" + uuid + "/promote")
                        .with(lead())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accuracy\":0.91}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POLICY_VIOLATION"));
    }

    @Test
    void promoteWithoutAccuracyReturns400AndServiceIsNotCalled() throws Exception {
        mvc.perform(post(BASE + "/" + UUID.randomUUID() + "/promote")
                        .with(lead())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value(containsString("accuracy")));

        verifyNoInteractions(promotion);
    }

    @Test
    void correlationIdFromRequestComesBackOn400() throws Exception {
        mvc.perform(post(BASE)
                        .with(scientist())
                        .header("X-Correlation-Id", "abc-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"ML-17\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId").value("abc-123"));
    }
}