package com.mlops.modellifecycle.handler;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MeResponse(
        @Schema(description = "The token's subject", example = "scientist")
        String username,

        @Schema(description = "Authorities from the token's roles claim, with the ROLE_ prefix",
                example = "[\"ROLE_DATA_SCIENTIST\"]")
        List<String> authorities
) {
}
