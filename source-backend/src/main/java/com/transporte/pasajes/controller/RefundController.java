package com.transporte.pasajes.controller;

import com.transporte.core.response.ApiResponse;
import com.transporte.core.response.PageResponse;
import com.transporte.pasajes.dto.RefundRequest;
import com.transporte.pasajes.dto.RefundResponse;
import com.transporte.pasajes.service.RefundService;
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
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "Ticket refund request management endpoints")
public class RefundController {

    private final RefundService refundService;

    @GetMapping
    @Operation(
            summary = "Get all pending refund requests paginated",
            description = "Returns a paginated list of all refund requests with PENDING status."
    )
    public ResponseEntity<ApiResponse<PageResponse<RefundResponse>>> findAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(refundService.findAll(pageable)));
    }

    @PostMapping
    @Operation(
            summary = "Request a refund for a ticket",
            description = "Creates a refund request for a confirmed ticket. " +
                    "An optional retention percentage can be specified to calculate the retained and refunded amounts. " +
                    "Only one refund request can exist per ticket."
    )
    public ResponseEntity<ApiResponse<RefundResponse>> requestRefund(
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Refund request created", refundService.requestRefund(request)));
    }

    @PostMapping("/{id}/approve")
    @Operation(
            summary = "Approve a refund request",
            description = "Approves a pending refund request. " +
                    "The associated ticket is automatically cancelled upon approval."
    )
    public ResponseEntity<ApiResponse<RefundResponse>> approve(
            @Parameter(description = "Refund UUID", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Refund approved", refundService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @Operation(
            summary = "Reject a refund request",
            description = "Rejects a pending refund request with an optional reason. " +
                    "The ticket remains in its current status."
    )
    public ResponseEntity<ApiResponse<RefundResponse>> reject(
            @Parameter(description = "Refund UUID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Reason for rejection")
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Refund rejected", refundService.reject(id, reason)));
    }
}
