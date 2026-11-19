package com.mlops.modellifecycle.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real filter chain, real dev users, real BCrypt: login and then use the token. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class LoginIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserDetailsService users;

    private MvcResult login(String user, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(user, password)))
                .andReturn();
    }

    @Test
    void devPasswordsAreStoredAsBcryptHashes() {
        for (String name : new String[]{DevUsersConfig.SCIENTIST_USER, DevUsersConfig.LEAD_USER}) {
            UserDetails user = users.loadUserByUsername(name);
            assertTrue(user.getPassword().startsWith("{bcrypt}"), name + " must be hashed");
        }
    }

    @Test
    void scientistLogsInAndTokenOpensTheApi() throws Exception {
        MvcResult result = login("scientist", "scientist-dev");
        assertEquals(200, result.getResponse().getStatus());

        String body = result.getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(body, "$.accessToken");
        assertEquals(900, (int) com.jayway.jsonpath.JsonPath.read(body, "$.expiresIn"));

        mvc.perform(get("/api/v1/models/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownUserAndWrongPasswordGetTheSameAnswer() throws Exception {
        MvcResult unknown = login("nobody", "whatever");
        MvcResult wrong = login("scientist", "wrong-password");

        assertEquals(401, unknown.getResponse().getStatus());
        assertEquals(401, wrong.getResponse().getStatus());
        for (MvcResult r : new MvcResult[]{unknown, wrong}) {
            String body = r.getResponse().getContentAsString();
            assertEquals("UNAUTHENTICATED", com.jayway.jsonpath.JsonPath.read(body, "$.code"));
            assertEquals("bad username or password", com.jayway.jsonpath.JsonPath.read(body, "$.message"));
        }
    }

    @Test
    void noTokenOnTheApiIs401() throws Exception {
        mvc.perform(get("/api/v1/models/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("valid token required"));
    }
}
