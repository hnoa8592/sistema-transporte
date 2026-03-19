package com.transporte.pasajes.dto;

import com.transporte.pasajes.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID ticketId,
        LocalDateTime expiresAt,
        ReservationStatus status,
        String notes,
        LocalDateTime createdAt
) {}
