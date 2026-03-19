package com.transporte.pasajes.dto;

import com.transporte.pasajes.enums.SaleType;
import com.transporte.pasajes.enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String ticketCode,
        UUID scheduleId,
        UUID customerId,
        String passengerName,
        String passengerDocument,
        int seatNumber,
        int floorNumber,
        LocalDate travelDate,
        BigDecimal price,
        TicketStatus status,
        SaleType saleType,
        UUID employeeId,
        LocalDateTime createdAt
) {}
