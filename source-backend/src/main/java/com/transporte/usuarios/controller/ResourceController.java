package com.transporte.usuarios.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.usuarios.dto.ResourceRequest;
import com.transporte.usuarios.dto.ResourceResponse;
import com.transporte.usuarios.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
@Tag(name = "Resources", description = "Resource management endpoints")
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    @Operation(summary = "Get all active resources paginated")
    public ResponseEntity<ApiResponse<PageResponse<ResourceResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(resourceService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource by ID")
    public ResponseEntity<ApiResponse<ResourceResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(resourceService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new resource")
    public ResponseEntity<ApiResponse<ResourceResponse>> create(@Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Resource created", resourceService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a resource")
    public ResponseEntity<ApiResponse<ResourceResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Resource updated", resourceService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a resource")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        resourceService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
