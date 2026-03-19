package com.transporte.usuarios.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        Set<ResourceResponse> resources,
        LocalDateTime createdAt
) {}
