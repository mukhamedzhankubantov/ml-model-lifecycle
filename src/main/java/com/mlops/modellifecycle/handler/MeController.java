package com.mlops.modellifecycle.handler;

import com.mlops.modellifecycle.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth", description = "Log in and get a short-lived access token")
public class MeController {

    private static final String FACTOR_PREFIX = "FACTOR_";

    @GetMapping("/api/me")
    @Operation(summary = "Who the token says you are",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @ApiResponse(responseCode = "200", description = "Username and authorities from the token",
            content = @Content(schema = @Schema(implementation = MeResponse.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED: no token, or the token is bad or expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
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
