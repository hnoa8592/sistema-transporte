package com.transporte.finanzas.dto;

import com.transporte.finanzas.enums.CashRegisterStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CashRegisterResponse(
        UUID id,
        UUID employeeId,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        BigDecimal initialAmount,
        BigDecimal finalAmount,
        CashRegisterStatus status,
        String notes,
        LocalDateTime createdAt
) {}
