package com.transporte.finanzas.dto;

import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InvoiceRequest(
        UUID customerId,
        String customerName,
        String customerDocument,
        BigDecimal taxPercent,
        @NotEmpty List<InvoiceItemRequest> items
) {}
