package com.transporte.usuarios.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResourceResponse(
        UUID id,
        String name,
        String httpMethod,
        String endpoint,
        String module,
        String description,
        boolean active,
        LocalDateTime createdAt
) {}
