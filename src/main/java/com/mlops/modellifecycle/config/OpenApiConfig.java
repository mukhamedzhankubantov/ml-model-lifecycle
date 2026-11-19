package com.mlops.modellifecycle.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the Bearer scheme once. Operations that need a token say
 * {@code @SecurityRequirement(name = OpenApiConfig.BEARER)}, so Swagger UI shows the lock
 * and an "Authorize" button where a token from /api/v1/auth/login can be pasted.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "ML Model Lifecycle API", version = "v1"))
@SecurityScheme(name = OpenApiConfig.BEARER, type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT",
        description = "Access token from POST /api/v1/auth/login")
public class OpenApiConfig {
    public static final String BEARER = "bearerAuth";
}
