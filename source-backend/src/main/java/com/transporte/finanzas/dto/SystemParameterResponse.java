package com.transporte.finanzas.dto;

import com.transporte.finanzas.enums.ParameterType;

import java.util.UUID;

public record SystemParameterResponse(
        UUID id,
        String key,
        String value,
        ParameterType type,
        String description,
        boolean active
) {}
