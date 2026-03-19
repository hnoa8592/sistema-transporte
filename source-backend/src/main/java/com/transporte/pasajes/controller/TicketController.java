package com.transporte.pasajes.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.pasajes.dto.*;
import com.transporte.pasajes.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Ticket purchase, management and cancellation endpoints")
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    @Operation(
            summary = "Get all tickets paginated",
            description = "Returns a paginated list of all non-cancelled tickets."
    )
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> findAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get ticket by ID",
            description = "Returns a single ticket by its UUID."
    )
    public ResponseEntity<ApiResponse<TicketResponse>> findById(
            @Parameter(description = "Ticket UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.findById(id)));
    }

    @GetMapping("/code/{code}")
    @Operation(
            summary = "Get ticket by code",
            description = "Returns a ticket by its unique ticket code (e.g. TKT-XXXXXXXX)."
    )
    public ResponseEntity<ApiResponse<TicketResponse>> findByCode(
            @Parameter(description = "Ticket code", required = true)
            @PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.findByCode(code)));
    }

    @PostMapping
    @Operation(
            summary = "Create (sell) a new ticket",
            description = "Creates a new ticket for a specific seat, schedule, and travel date. " +
                    "Automatically updates the seat map marking the seat as SOLD."
    )
    public ResponseEntity<ApiResponse<TicketResponse>> create(
            @Valid @RequestBody TicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Ticket created successfully", ticketService.create(request)));
    }

    @PostMapping("/bulk")
    @Operation(
            summary = "Create multiple tickets in a single transaction",
            description = "Creates multiple tickets atomically. If any seat is unavailable, the entire operation is rolled back."
    )
    public ResponseEntity<ApiResponse<List<TicketResponse>>> createBulk(
            @Valid @RequestBody BulkTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tickets created successfully", ticketService.createBulk(request)));
    }

    @PatchMapping("/{id}/customer")
    @Operation(
            summary = "Update passenger/customer information on a ticket",
            description = "Allows updating the customer ID, passenger name, and passenger document on an existing ticket."
    )
    public ResponseEntity<ApiResponse<TicketResponse>> updateCustomerInfo(
            @Parameter(description = "Ticket UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketCustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket customer info updated", ticketService.updateCustomerInfo(id, request)));
    }

    @PatchMapping("/{id}/seat")
    @Operation(
            summary = "Change seat on an existing ticket",
            description = "Changes the assigned seat for a ticket. " +
                    "The old seat is released (AVAILABLE) and the new seat is marked as SOLD."
    )
    public ResponseEntity<ApiResponse<TicketResponse>> changeSeat(
            @Parameter(description = "Ticket UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody ChangeSeatRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Seat changed successfully", ticketService.changeSeat(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Cancel a ticket",
            description = "Cancels a ticket and releases the associated seat back to AVAILABLE status."
    )
    public ResponseEntity<ApiResponse<Void>> cancel(
            @Parameter(description = "Ticket UUID", required = true)
            @PathVariable UUID id) {
        ticketService.cancel(id);
        return ResponseEntity.ok(ApiResponse.ok("Ticket cancelled successfully", null));
    }
}
