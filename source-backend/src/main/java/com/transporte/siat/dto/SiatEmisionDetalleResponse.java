package com.transporte.siat.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SiatEmisionDetalleResponse(
        UUID id,
        Integer numeroLinea,
        String actividadEconomica,
        Integer codigoProductoSin,
        String codigoProducto,
        String descripcion,
        BigDecimal cantidad,
        Integer unidadMedida,
        BigDecimal precioUnitario,
        BigDecimal montoDescuento,
        BigDecimal subTotal
) {}
