package com.transporte.operacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record LocationRequest(
        @NotBlank String name,
        @NotNull UUID provinceId,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean active
) {}
