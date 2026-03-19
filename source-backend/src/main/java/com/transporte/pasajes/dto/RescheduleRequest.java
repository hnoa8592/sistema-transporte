package com.transporte.pasajes.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RescheduleRequest(
        @NotNull(message = "Original ticket ID is required") UUID originalTicketId,
        @NotNull(message = "New schedule ID is required") UUID newScheduleId,
        @NotNull(message = "New travel date is required") LocalDate newTravelDate,
        int newSeatNumber,
        int newFloorNumber,
        String reason,
        BigDecimal fee,
        UUID employeeId
) {}
