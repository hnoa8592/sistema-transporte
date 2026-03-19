package com.transporte.siat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SiatConfigRequest(
        @NotBlank String nit,
        @NotBlank String razonSocial,
        @NotBlank String codigoSistema,
        @NotBlank String codigoActividad,
        @NotNull Integer codigoSucursal,
        Integer codigoPuntoVenta,
        String direccion,
        String municipio,
        String telefono,
        @NotNull Integer codigoAmbiente,
        @NotNull Integer codigoModalidad
) {}
