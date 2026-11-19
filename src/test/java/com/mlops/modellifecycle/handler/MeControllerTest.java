package com.mlops.modellifecycle.handler;

import com.mlops.modellifecycle.security.Roles;
import com.mlops.modellifecycle.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeController.class)
@Import(SecurityConfig.class)
class MeControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void returnsNameAndAuthoritiesFromTheToken() throws Exception {
        mvc.perform(get("/api/me")
                        .with(jwt().jwt(j -> j.subject("lead"))
                                .authorities(new SimpleGrantedAuthority("ROLE_" + Roles.LEAD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("lead"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_ML_LEAD"))
                .andExpect(jsonPath("$.authorities.length()").value(1));
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("valid token required"));
    }

    @Test
    void usernameInTheQueryStringIsIgnored() throws Exception {
        mvc.perform(get("/api/me").param("username", "mallory")
                        .with(jwt().jwt(j -> j.subject("scientist"))
                                .authorities(new SimpleGrantedAuthority("ROLE_" + Roles.SCIENTIST))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("scientist"));
    }
}
