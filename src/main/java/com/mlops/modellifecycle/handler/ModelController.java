package com.mlops.modellifecycle.handler;

import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelItem;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.service.ModelRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

    public ModelController(ModelRegistrationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Register a new model in DRAFT status")
    @ApiResponse(responseCode = "201", description = "Created; Location points to the new model",
            content = @Content(schema = @Schema(implementation = ModelResponse.class)))
    @ApiResponse(responseCode = "400", description = "VALIDATION: a field is blank, too long, or the body is not JSON",
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
    @Operation(summary = "Read one model by its UUID")
    @ApiResponse(responseCode = "200", description = "The model",
            content = @Content(schema = @Schema(implementation = ModelResponse.class)))
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: no model with this UUID",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ModelResponse get(@PathVariable UUID id) {
        return ModelResponse.from(service.get(new ModelId(id)));
    }
}