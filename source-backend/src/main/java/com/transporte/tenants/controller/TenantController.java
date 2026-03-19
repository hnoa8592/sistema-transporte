package com.transporte.tenants.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.tenants.dto.TenantRequest;
import com.transporte.tenants.dto.TenantResponse;
import com.transporte.tenants.dto.TenantStatusRequest;
import com.transporte.tenants.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant management endpoints (SaaS administration)")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @Operation(
            summary = "List all tenants",
            description = "Returns a paginated list of all tenants registered in the system."
    )
    public ResponseEntity<ApiResponse<PageResponse<TenantResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get tenant by ID",
            description = "Returns a single tenant by its UUID."
    )
    public ResponseEntity<ApiResponse<TenantResponse>> findById(
            @Parameter(description = "Tenant UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.findById(id)));
    }

    @GetMapping("/schema/{schemaName}")
    @Operation(
            summary = "Get tenant by schema name",
            description = "Returns a tenant identified by its PostgreSQL schema name."
    )
    public ResponseEntity<ApiResponse<TenantResponse>> findBySchemaName(
            @Parameter(description = "Tenant schema name", required = true)
            @PathVariable String schemaName) {
        return ResponseEntity.ok(ApiResponse.ok(tenantService.findBySchemaName(schemaName)));
    }

    @PostMapping
    @Operation(
            summary = "Create a new tenant",
            description = "Creates a new tenant and provisions its PostgreSQL schema by running all tenant Flyway migrations."
    )
    public ResponseEntity<ApiResponse<TenantResponse>> create(
            @Valid @RequestBody TenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tenant created", tenantService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update tenant data",
            description = "Updates the tenant's information. The schema name cannot be changed after creation."
    )
    public ResponseEntity<ApiResponse<TenantResponse>> update(
            @Parameter(description = "Tenant UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody TenantRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tenant updated", tenantService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Change tenant status",
            description = "Updates the tenant status (ACTIVE, INACTIVE, SUSPENDED) without reprovisioning the schema."
    )
    public ResponseEntity<ApiResponse<TenantResponse>> updateStatus(
            @Parameter(description = "Tenant UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody TenantStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tenant status updated", tenantService.updateStatus(id, request)));
    }
}
