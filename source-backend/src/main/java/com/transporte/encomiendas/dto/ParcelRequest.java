package com.transporte.encomiendas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ParcelRequest(
        UUID senderId,
        @NotBlank(message = "Sender name is required") String senderName,
        String senderPhone,
        UUID recipientId,
        @NotBlank(message = "Recipient name is required") String recipientName,
        String recipientPhone,
        UUID scheduleId,
        String description,
        @NotNull(message = "Weight is required") BigDecimal weight,
        BigDecimal declaredValue,
        @NotNull(message = "Price is required") BigDecimal price,
        UUID employeeId
) {}
