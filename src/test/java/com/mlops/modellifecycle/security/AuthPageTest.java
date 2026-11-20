package com.mlops.modellifecycle.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The page is served by this process, through the real filter chain, with no token. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthPageTest {

    @Autowired
    MockMvc mvc;

    @Test
    void rootServesTheOwnPageNotASpringLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void indexHasTheProductRolesAndDevUsers() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ML Model Lifecycle")))
                .andExpect(content().string(containsString("DATA_SCIENTIST")))
                .andExpect(content().string(containsString("ML_LEAD")))
                .andExpect(content().string(containsString("scientist-dev")))
                .andExpect(content().string(containsString("dev only")))
                .andExpect(content().string(containsString("type=\"password\"")))
                .andExpect(content().string(not(containsString("Please sign in"))));
    }

    @Test
    void cssAndScriptAreOpen() throws Exception {
        mvc.perform(get("/styles.css")).andExpect(status().isOk());
        mvc.perform(get("/app.js")).andExpect(status().isOk());
    }

    @Test
    void everyImageTheHomePageUsesIsServedWithNoToken() throws Exception {
        String html = mvc.perform(get("/index.html")).andReturn().getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("src=\"(img/[^\"]+)\"").matcher(html);

        int count = 0;
        while (m.find()) {
            count++;
            mvc.perform(get("/" + m.group(1)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("image/svg+xml"));
        }
        org.junit.jupiter.api.Assertions.assertTrue(count >= 8, "expected the page to use its images, found " + count);
    }

    @Test
    void scriptKeepsTheTokenOutOfBrowserStorageAndHtml() throws Exception {
        String js = mvc.perform(get("/app.js")).andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertFalse(js.contains("localStorage"), "localStorage"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(js.contains("sessionStorage"), "sessionStorage"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(js.contains("document.cookie"), "cookies"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(js.contains("innerHTML"), "innerHTML"),
                () -> org.junit.jupiter.api.Assertions.assertFalse(js.contains("location.hash"), "token in URL"),
                () -> org.junit.jupiter.api.Assertions.assertTrue(js.contains("/api/v1/auth/login")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(js.contains("X-Correlation-Id")),
                () -> org.junit.jupiter.api.Assertions.assertTrue(js.contains("Bearer ")));
    }

    @Test
    void openingThePageDoesNotOpenTheApi() throws Exception {
        mvc.perform(get("/api/v1/models/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }
}
