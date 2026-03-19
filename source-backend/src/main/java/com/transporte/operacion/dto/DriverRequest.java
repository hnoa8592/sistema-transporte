package com.transporte.operacion.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record DriverRequest(
        @NotBlank(message = "DNI is required") String dni,
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Last name is required") String lastName,
        String licenseNumber,
        String licenseCategory,
        LocalDate licenseExpiryDate,
        String phone,
        String email,
        boolean active
) {}
