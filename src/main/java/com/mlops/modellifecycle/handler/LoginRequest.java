package com.mlops.modellifecycle.handler;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "User name", example = "scientist", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "username must not be blank")
        String username,

        @Schema(description = "Password", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "password must not be blank")
        String password
) {
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
