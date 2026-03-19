package com.transporte.operacion.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.BusRequest;
import com.transporte.operacion.dto.BusResponse;
import com.transporte.operacion.service.BusService;
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
@RequestMapping("/api/v1/buses")
@RequiredArgsConstructor
@Tag(name = "Buses", description = "Bus management endpoints")
public class BusController {

    private final BusService busService;

    @GetMapping
    @Operation(summary = "Get all active buses paginated")
    public ResponseEntity<ApiResponse<PageResponse<BusResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(busService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bus by ID")
    public ResponseEntity<ApiResponse<BusResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(busService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new bus")
    public ResponseEntity<ApiResponse<BusResponse>> create(@Valid @RequestBody BusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Bus created", busService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a bus")
    public ResponseEntity<ApiResponse<BusResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody BusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Bus updated", busService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a bus")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        busService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
