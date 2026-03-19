package com.transporte.operacion.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.DriverRequest;
import com.transporte.operacion.dto.DriverResponse;
import com.transporte.operacion.service.DriverService;
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
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Drivers", description = "Driver management endpoints")
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    @Operation(summary = "Get all active drivers paginated")
    public ResponseEntity<ApiResponse<PageResponse<DriverResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(driverService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get driver by ID")
    public ResponseEntity<ApiResponse<DriverResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(driverService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new driver")
    public ResponseEntity<ApiResponse<DriverResponse>> create(@Valid @RequestBody DriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Driver created", driverService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a driver")
    public ResponseEntity<ApiResponse<DriverResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Driver updated", driverService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a driver")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        driverService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
