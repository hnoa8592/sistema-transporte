package com.transporte.operacion.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.FleetRequest;
import com.transporte.operacion.dto.FleetResponse;
import com.transporte.operacion.service.FleetService;
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
@RequestMapping("/api/v1/fleets")
@RequiredArgsConstructor
@Tag(name = "Fleets", description = "Fleet management endpoints")
public class FleetController {

    private final FleetService fleetService;

    @GetMapping
    @Operation(summary = "Get all fleets paginated")
    public ResponseEntity<ApiResponse<PageResponse<FleetResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(fleetService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fleet by ID")
    public ResponseEntity<ApiResponse<FleetResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(fleetService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new fleet")
    public ResponseEntity<ApiResponse<FleetResponse>> create(@Valid @RequestBody FleetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Fleet created", fleetService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a fleet")
    public ResponseEntity<ApiResponse<FleetResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody FleetRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Fleet updated", fleetService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a fleet")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        fleetService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
