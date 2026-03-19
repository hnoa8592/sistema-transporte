package com.transporte.pasajes.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.pasajes.dto.RescheduleRequest;
import com.transporte.pasajes.dto.RescheduleResponse;
import com.transporte.pasajes.service.RescheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reschedules")
@RequiredArgsConstructor
@Tag(name = "Reschedules", description = "Ticket rescheduling management endpoints")
public class RescheduleController {

    private final RescheduleService rescheduleService;

    @GetMapping
    @Operation(
            summary = "Get all reschedules paginated",
            description = "Returns a paginated list of all ticket reschedule records, ordered by creation date descending."
    )
    public ResponseEntity<ApiResponse<PageResponse<RescheduleResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(rescheduleService.findAll(pageable)));
    }

    @PostMapping
    @Operation(
            summary = "Reschedule a ticket",
            description = "Reschedules a confirmed ticket to a new schedule, travel date, and optionally a new seat. " +
                    "The original ticket is marked as RESCHEDULED and a new ticket is created for the new schedule. " +
                    "An optional rescheduling fee can be specified."
    )
    public ResponseEntity<ApiResponse<RescheduleResponse>> reschedule(
            @Valid @RequestBody RescheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Ticket rescheduled successfully", rescheduleService.reschedule(request)));
    }
}
