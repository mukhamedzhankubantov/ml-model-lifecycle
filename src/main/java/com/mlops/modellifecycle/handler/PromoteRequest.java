package com.mlops.modellifecycle.handler;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Only the measured accuracy. Who promotes comes from the token, never from this body. */
public record PromoteRequest(
        @Schema(description = "Measured accuracy of the model, 0 to 1", example = "0.91",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "accuracy must not be null")
        @DecimalMin(value = "0.0", message = "accuracy must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "accuracy must be between 0 and 1")
        Double accuracy
) {
}
