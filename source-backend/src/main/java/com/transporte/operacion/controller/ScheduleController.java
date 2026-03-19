package com.transporte.operacion.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.ScheduleRequest;
import com.transporte.operacion.dto.ScheduleResponse;
import com.transporte.operacion.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Schedule management endpoints")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    @Operation(summary = "Get all active schedules paginated")
    public ResponseEntity<ApiResponse<PageResponse<ScheduleResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<ApiResponse<ScheduleResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.findById(id)));
    }

    @GetMapping("/route/{routeId}")
    @Operation(summary = "Get schedules by route ID")
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> findByRoute(@PathVariable UUID routeId) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.findByRoute(routeId)));
    }

    @PostMapping
    @Operation(summary = "Create a new schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> create(@Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Schedule created", scheduleService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a schedule")
    public ResponseEntity<ApiResponse<ScheduleResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Schedule updated", scheduleService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a schedule")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        scheduleService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
