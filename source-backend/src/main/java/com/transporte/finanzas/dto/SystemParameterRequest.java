package com.transporte.finanzas.dto;

import com.transporte.finanzas.enums.ParameterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SystemParameterRequest(
        @NotBlank String key,
        @NotBlank String value,
        @NotNull ParameterType type,
        String description,
        boolean active
) {}
