package com.mlops.modellifecycle.handler;

import com.mlops.modellifecycle.domain.DuplicateModel;
import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelItem;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.domain.ModelNotFound;
import com.mlops.modellifecycle.domain.ModelStatus;
import com.mlops.modellifecycle.service.ModelRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModelController.class)
class ModelControllerTest {

    private static final String BASE = "/api/v1/models";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ModelRegistrationService service;

    @Test
    void validBodyReturns201WithLocation() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(service.register(any(ModelId.class), any(ModelKey.class), eq("Churn model")))
                .thenReturn(new ModelItem(new ModelId(uuid), new ModelKey("ML-17"),
                        ModelStatus.DRAFT, "Churn model"));

        mvc.perform(post(BASE)
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

        mvc.perform(get(BASE + "/" + uuid))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void duplicateKeyReturns409() throws Exception {
        when(service.register(any(ModelId.class), any(ModelKey.class), any(String.class)))
                .thenThrow(new DuplicateModel("ML-17"));

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"ML-17\",\"name\":\"Churn again\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"));
    }

    @Test
    void businessKeyInPathReturns400() throws Exception {
        mvc.perform(get(BASE + "/ML-17"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("id must be a UUID"));

        verifyNoInteractions(service);
    }

    @Test
    void correlationIdFromRequestComesBackOn400() throws Exception {
        mvc.perform(post(BASE)
                        .header("X-Correlation-Id", "abc-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"ML-17\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId").value("abc-123"));
    }
}