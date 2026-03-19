package com.transporte.operacion.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.LocationRequest;
import com.transporte.operacion.dto.LocationResponse;
import com.transporte.operacion.service.LocationService;
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
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Location management endpoints")
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    @Operation(summary = "Get all active locations paginated")
    public ResponseEntity<ApiResponse<PageResponse<LocationResponse>>> findAll(
            @PageableDefault(size = 200) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get location by ID")
    public ResponseEntity<ApiResponse<LocationResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new location")
    public ResponseEntity<ApiResponse<LocationResponse>> create(@Valid @RequestBody LocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Location created", locationService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a location")
    public ResponseEntity<ApiResponse<LocationResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Location updated", locationService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a location")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        locationService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
