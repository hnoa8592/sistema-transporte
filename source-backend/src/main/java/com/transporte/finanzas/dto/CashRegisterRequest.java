package com.transporte.finanzas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record CashRegisterRequest(
        @NotNull(message = "Employee ID is required") UUID employeeId,
        @NotNull(message = "Initial amount is required") @PositiveOrZero BigDecimal initialAmount,
        String notes
) {}
