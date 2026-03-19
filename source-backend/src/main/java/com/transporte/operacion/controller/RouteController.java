package com.transporte.operacion.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.RouteRequest;
import com.transporte.operacion.dto.RouteResponse;
import com.transporte.operacion.service.RouteService;
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
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Tag(name = "Routes", description = "Route management endpoints")
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    @Operation(summary = "Get all active routes paginated")
    public ResponseEntity<ApiResponse<PageResponse<RouteResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(routeService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get route by ID")
    public ResponseEntity<ApiResponse<RouteResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(routeService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new route")
    public ResponseEntity<ApiResponse<RouteResponse>> create(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Route created", routeService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a route")
    public ResponseEntity<ApiResponse<RouteResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Route updated", routeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a route")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        routeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
