package com.mlops.modellifecycle.handler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth", description = "Log in and get a short-lived access token")
public class MeController {

    /**
     * Reads the Authentication that the filter chain already built from the verified token.
     * Nothing here comes from the request body or from a client-supplied header.
     */
    // Spring Security 7 adds FACTOR_BEARER to bearer-token logins to say how the caller
    // authenticated. It is not in the token, so it is left out of "authorities from the token".
    private static final String FACTOR_PREFIX = "FACTOR_";

    @GetMapping("/api/me")
    @Operation(summary = "Who the token says you are")
    public MeResponse me(Authentication authentication) {
        return new MeResponse(
                authentication.getName(),
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(a -> !a.startsWith(FACTOR_PREFIX))
                        .sorted()
                        .toList());
    }
}
