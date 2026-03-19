package com.transporte.operacion.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DriverResponse(
        UUID id,
        String dni,
        String firstName,
        String lastName,
        String licenseNumber,
        String licenseCategory,
        LocalDate licenseExpiryDate,
        String phone,
        String email,
        boolean active,
        LocalDateTime createdAt
) {}
