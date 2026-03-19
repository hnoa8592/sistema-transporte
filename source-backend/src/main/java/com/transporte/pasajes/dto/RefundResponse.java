package com.transporte.pasajes.dto;

import com.transporte.pasajes.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID ticketId,
        String reason,
        BigDecimal retentionPercent,
        BigDecimal originalAmount,
        BigDecimal retainedAmount,
        BigDecimal refundedAmount,
        RefundStatus status,
        LocalDateTime createdAt
) {}
