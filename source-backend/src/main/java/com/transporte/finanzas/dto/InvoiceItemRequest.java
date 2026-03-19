package com.transporte.finanzas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record InvoiceItemRequest(
        @NotBlank String description,
        @Positive int quantity,
        @Positive BigDecimal unitPrice
) {}
