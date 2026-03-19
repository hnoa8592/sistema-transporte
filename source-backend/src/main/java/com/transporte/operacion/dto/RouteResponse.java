package com.transporte.operacion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RouteResponse(
        UUID id,
        UUID originLocationId,
        String originLocationName,
        UUID destinationLocationId,
        String destinationLocationName,
        BigDecimal distanceKm,
        Integer estimatedDurationMinutes,
        BigDecimal basePrice,
        boolean active,
        String description,
        LocalDateTime createdAt
) {}
