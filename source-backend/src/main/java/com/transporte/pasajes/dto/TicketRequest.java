package com.transporte.pasajes.dto;

import com.transporte.pasajes.enums.SaleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TicketRequest(
        @NotNull(message = "Schedule ID is required") UUID scheduleId,
        UUID customerId,
        @NotNull(message = "Seat number is required") @Positive int seatNumber,
        @NotNull(message = "Floor number is required") int floorNumber,
        @NotNull(message = "Travel date is required") LocalDate travelDate,
        @NotNull(message = "Price is required") BigDecimal price,
        SaleType saleType,
        UUID employeeId,
        String passengerName,
        String passengerDocument,
        String passengerDocumentType
) {}
