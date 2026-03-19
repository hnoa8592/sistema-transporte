package com.transporte.finanzas.dto;

import com.transporte.finanzas.enums.ReferenceType;
import com.transporte.finanzas.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CashTransactionRequest(
        @NotNull UUID cashRegisterId,
        @NotNull TransactionType type,
        String concept,
        @NotNull @Positive BigDecimal amount,
        UUID referenceId,
        ReferenceType referenceType
) {}
