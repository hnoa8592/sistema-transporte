package com.transporte.pasajes.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequest(
        @NotNull(message = "Ticket ID is required") UUID ticketId,
        String reason,
        BigDecimal retentionPercent,
        UUID employeeId
) {}
