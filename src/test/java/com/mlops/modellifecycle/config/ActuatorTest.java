package com.mlops.modellifecycle.config;

import com.mlops.modellifecycle.security.Roles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs through the real security filter chain (the chain is not switched off here).
 * Only /actuator/health is open; every other actuator URL falls under "anything else".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ActuatorTest {

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsUpWithoutAToken() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void infoIsBehindTheChain() throws Exception {
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void envIsClosedWithoutAToken() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void envIsClosedEvenForTheSeniorRole() throws Exception {
        mvc.perform(get("/actuator/env").with(lead()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void beansIsClosedWithoutAToken() throws Exception {
        mvc.perform(get("/actuator/beans")).andExpect(status().isUnauthorized());
    }

    @Test
    void heapdumpIsClosedEvenForTheSeniorRole() throws Exception {
        mvc.perform(get("/actuator/heapdump").with(lead()))
                .andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor lead() {
        return jwt().jwt(j -> j.subject("lead"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + Roles.LEAD));
    }
}
