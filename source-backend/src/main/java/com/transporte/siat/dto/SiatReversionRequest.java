package com.transporte.siat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SiatReversionRequest(
        @NotNull UUID siatFacturaId
) {}
