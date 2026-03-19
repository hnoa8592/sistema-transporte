package com.transporte.usuarios.dto;

import jakarta.validation.constraints.Email;

import java.util.UUID;

public record UpdateUserRequest(
        @Email(message = "Invalid email format") String email,
        String firstName,
        String lastName,
        Boolean active,
        UUID profileId
) {}
