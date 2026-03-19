package com.transporte.operacion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        String name,
        String city,
        String department,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean active,
        LocalDateTime createdAt
) {}
