package com.transporte.usuarios.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean active,
        UUID profileId,
        String profileName,
        LocalDateTime createdAt
) {}
