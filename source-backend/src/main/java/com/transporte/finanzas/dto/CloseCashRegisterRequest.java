package com.transporte.finanzas.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CloseCashRegisterRequest(
        @PositiveOrZero BigDecimal finalAmount,
        String notes
) {}
