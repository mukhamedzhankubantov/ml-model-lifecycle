package com.mlops.modellifecycle.handler;

import com.mlops.modellifecycle.domain.ModelId;
import com.mlops.modellifecycle.domain.ModelItem;
import com.mlops.modellifecycle.domain.ModelKey;
import com.mlops.modellifecycle.service.ModelRegistrationService;
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
public class ModelController {
    private final ModelRegistrationService service;

    public ModelController(ModelRegistrationService service) {
        this.service = service;
    }

    @PostMapping
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
    public ModelResponse get(@PathVariable UUID id) {
        return ModelResponse.from(service.get(new ModelId(id)));
    }
}