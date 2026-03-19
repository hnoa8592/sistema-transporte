package com.transporte.siat.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SiatEmisionDetalleRequest(
        String actividadEconomica,
        Integer codigoProductoSin,
        String codigoProducto,
        @NotBlank String descripcion,
        @NotNull @DecimalMin("0.0001") BigDecimal cantidad,
        Integer unidadMedida,
        @NotNull @DecimalMin("0.00") BigDecimal precioUnitario,
        BigDecimal montoDescuento
) {}
