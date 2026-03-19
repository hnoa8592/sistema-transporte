package com.transporte.encomiendas.dto;

import com.transporte.encomiendas.enums.ParcelStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ParcelResponse(
        UUID id,
        String trackingCode,
        UUID senderId,
        String senderName,
        String senderPhone,
        UUID recipientId,
        String recipientName,
        String recipientPhone,
        UUID scheduleId,
        String description,
        BigDecimal weight,
        BigDecimal declaredValue,
        BigDecimal price,
        ParcelStatus status,
        UUID employeeId,
        LocalDateTime createdAt
) {}
