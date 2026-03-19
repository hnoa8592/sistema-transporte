package com.transporte.pasajes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RescheduleResponse(
        UUID id,
        UUID originalTicketId,
        UUID newTicketId,
        UUID newScheduleId,
        String reason,
        BigDecimal fee,
        LocalDateTime createdAt
) {}
