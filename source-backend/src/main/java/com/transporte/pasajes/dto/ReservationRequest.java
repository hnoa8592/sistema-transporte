package com.transporte.pasajes.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReservationRequest(
        @NotNull(message = "Ticket ID is required") UUID ticketId,
        String notes
) {}
