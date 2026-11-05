package com.mlops.modellifecycle.handler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateModelRequest(
        @NotBlank(message = "business key must not be blank")
        String businessKey,

        @NotBlank(message = "name must not be blank")
        @Size(max = 120, message = "name size must be between 0 and 120")
        String name
) {
}