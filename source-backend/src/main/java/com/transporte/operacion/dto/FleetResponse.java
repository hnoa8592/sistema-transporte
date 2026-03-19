package com.transporte.operacion.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FleetResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt
) {}
