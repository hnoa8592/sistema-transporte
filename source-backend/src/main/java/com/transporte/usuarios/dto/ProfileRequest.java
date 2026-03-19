package com.transporte.usuarios.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

public record ProfileRequest(
        @NotBlank(message = "Name is required") String name,
        String description,
        boolean active,
        Set<UUID> resourceIds
) {}
