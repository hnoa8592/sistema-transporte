package com.transporte.siat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SiatAnulacionRequest(
        @NotNull UUID siatFacturaId,
        /** Código de motivo de anulación del catálogo SIAT */
        @NotNull Integer codigoMotivo
) {}
