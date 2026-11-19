package com.mlops.modellifecycle.handler;

import com.mlops.modellifecycle.config.OpenApiConfig;
import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelItem;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.service.ModelPromotionService;
import com.mlops.modellifecycle.service.ModelRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/models")
@Tag(name = "Models", description = "Register and read ML models")
public class ModelController {
    private final ModelRegistrationService service;
    private final ModelPromotionService promotion;

    public ModelController(ModelRegistrationService service, ModelPromotionService promotion) {
        this.service = service;
        this.promotion = promotion;
    }

    @PostMapping
    @Operation(summary = "Register a new model in DRAFT status",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @ApiResponse(responseCode = "201", description = "Created; Location points to the new model",
            content = @Content(schema = @Schema(implementation = ModelResponse.class)))
    @ApiResponse(responseCode = "400", description = "VALIDATION: a field is blank, too long, or the body is not JSON",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED: no token, or the token is bad or expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "DUPLICATE: the business key is already registered",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<ModelResponse> create(@Valid @RequestBody CreateModelRequest request) {
        ModelItem item = service.register(
                ModelId.newId(),
                new ModelKey(request.businessKey()),
                request.name());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(item.id().getValue())
                .toUri();

        return ResponseEntity.created(location).body(ModelResponse.from(item));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read one model by its UUID",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @ApiResponse(responseCode = "200", description = "The model",
            content = @Content(schema = @Schema(implementation = ModelResponse.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED: no token, or the token is bad or expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: no model with this UUID",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ModelResponse get(@PathVariable UUID id) {
        return ModelResponse.from(service.get(new ModelId(id)));
    }

    @PostMapping("/{id}/promote")
    @Operation(summary = "Promote a model to PRODUCTION (ML_LEAD only)",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @ApiResponse(responseCode = "200", description = "Promoted; the body carries the new status",
            content = @Content(schema = @Schema(implementation = ModelResponse.class)))
    @ApiResponse(responseCode = "400", description = "VALIDATION: accuracy is missing, outside 0..1, or the id is not a UUID",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED: no token, or the token is bad or expired",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: the token's role is not ML_LEAD; the model is unchanged",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: no model with this UUID",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "POLICY_VIOLATION: the policy forbids this move; the model is unchanged",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ModelResponse promote(@PathVariable UUID id,
                                 @Valid @RequestBody PromoteRequest request,
                                 Authentication authentication) {
        // The actor is the authenticated name from the token, not a JSON field.
        return ModelResponse.from(
                promotion.promote(new ModelId(id), authentication.getName(), request.accuracy()));
    }
}