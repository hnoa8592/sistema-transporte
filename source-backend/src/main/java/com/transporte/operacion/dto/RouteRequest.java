package com.transporte.operacion.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RouteRequest(
        @NotNull(message = "Origin location is required") UUID originLocationId,
        @NotNull(message = "Destination location is required") UUID destinationLocationId,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        BigDecimal basePrice,
        boolean active,
        String description
) {}
