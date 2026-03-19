package com.transporte.finanzas.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID id,
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
