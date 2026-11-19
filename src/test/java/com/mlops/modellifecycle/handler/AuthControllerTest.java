package com.mlops.modellifecycle.handler;

import com.mlops.modellifecycle.security.Roles;
import com.mlops.modellifecycle.security.SecurityConfig;
import com.mlops.modellifecycle.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String LOGIN = "/api/v1/auth/login";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthenticationManager authenticationManager;

    @MockitoBean
    TokenService tokenService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void rightPasswordReturnsBearerTokenWithoutAnyToken() throws Exception {
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated("lead", null,
                        AuthorityUtils.createAuthorityList("ROLE_" + Roles.LEAD)));
        when(tokenService.issue(any())).thenReturn("signed.jwt.value");
        when(tokenService.expiresInSeconds()).thenReturn(900L);

        mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"lead\",\"password\":\"lead-dev\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void wrongPasswordReturns401WithTheSharedMessage() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

        mvc.perform(post(LOGIN)
                        .header("X-Correlation-Id", "login-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"lead\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("bad username or password"))
                .andExpect(jsonPath("$.correlationId").value("login-1"));
    }

    @Test
    void blankPasswordReturns400AndManagerIsNotCalled() throws Exception {
        mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"lead\",\"password\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("password must not be blank"));

        verifyNoInteractions(authenticationManager, tokenService);
    }

    @Test
    void blankUsernameReturns400() throws Exception {
        mvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("username must not be blank"));

        verifyNoInteractions(authenticationManager);
    }
}
