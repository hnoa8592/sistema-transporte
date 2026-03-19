package com.transporte.finanzas.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CashRegisterSummaryResponse(
        UUID cashRegisterId,
        BigDecimal initialAmount,
        BigDecimal totalIncomes,
        BigDecimal totalExpenses,
        BigDecimal expectedFinalAmount
) {}
