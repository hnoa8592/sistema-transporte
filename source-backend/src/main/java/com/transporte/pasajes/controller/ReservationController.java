package com.transporte.pasajes.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.pasajes.dto.ReservationRequest;
import com.transporte.pasajes.dto.ReservationResponse;
import com.transporte.pasajes.enums.ReservationStatus;
import com.transporte.pasajes.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Seat reservation management endpoints")
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    @Operation(
            summary = "List all reservations",
            description = "Returns a paginated list of reservations, optionally filtered by status."
    )
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ReservationStatus status) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok("OK", reservationService.findAll(pageable, status)));
    }

    @PostMapping
    @Operation(
            summary = "Create a seat reservation",
            description = "Creates a temporary reservation for a ticket seat. " +
                    "The seat is marked as RESERVED and the reservation expires after the configured timeout (default 30 minutes)."
    )
    public ResponseEntity<ApiResponse<ReservationResponse>> create(
            @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Reservation created", reservationService.create(request)));
    }

    @PostMapping("/{id}/confirm")
    @Operation(
            summary = "Confirm a reservation",
            description = "Confirms a pending reservation before it expires. " +
                    "The seat status is updated from RESERVED to SOLD."
    )
    public ResponseEntity<ApiResponse<ReservationResponse>> confirm(
            @Parameter(description = "Reservation UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation confirmed", reservationService.confirm(id)));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancel a reservation",
            description = "Cancels a pending reservation and releases the seat back to AVAILABLE status."
    )
    public ResponseEntity<ApiResponse<Void>> cancel(
            @Parameter(description = "Reservation UUID", required = true)
            @PathVariable UUID id) {
        reservationService.cancel(id);
        return ResponseEntity.ok(ApiResponse.ok("Reservation cancelled", null));
    }
}
