package com.transporte.pasajes.dto;

import com.transporte.pasajes.enums.SeatStatus;

import java.time.LocalDate;
import java.util.UUID;

public record SeatMapResponse(
        UUID id,
        UUID scheduleId,
        LocalDate travelDate,
        int seatNumber,
        int floorNumber,
        SeatStatus status,
        UUID ticketId
) {}
