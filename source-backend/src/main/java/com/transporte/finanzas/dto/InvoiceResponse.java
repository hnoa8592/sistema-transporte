package com.transporte.finanzas.dto;

import com.transporte.finanzas.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        UUID customerId,
        String customerName,
        String customerDocument,
        BigDecimal subtotal,
        BigDecimal taxPercent,
        BigDecimal taxAmount,
        BigDecimal total,
        InvoiceStatus status,
        List<InvoiceItemResponse> items,
        LocalDateTime createdAt
) {}
