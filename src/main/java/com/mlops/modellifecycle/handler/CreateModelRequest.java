package com.mlops.modellifecycle.handler;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateModelRequest(
        @Schema(description = "Human-readable number, unique", example = "ML-17",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "business key must not be blank")
        String businessKey,

        @Schema(description = "Model name, up to 120 characters", example = "Churn model",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 120)
        @NotBlank(message = "name must not be blank")
        @Size(max = 120, message = "name size must be between 0 and 120")
        String name
) {
}