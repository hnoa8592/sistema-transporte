package com.transporte.finanzas.dto;

import com.transporte.finanzas.enums.ReferenceType;
import com.transporte.finanzas.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CashTransactionResponse(
        UUID id,
        UUID cashRegisterId,
        TransactionType type,
        String concept,
        BigDecimal amount,
        UUID referenceId,
        ReferenceType referenceType,
        LocalDateTime createdAt
) {}
