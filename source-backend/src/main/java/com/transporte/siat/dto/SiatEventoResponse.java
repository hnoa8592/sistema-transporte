package com.transporte.siat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SiatEventoResponse(
        UUID id,
        Integer codigoEvento,
        String descripcion,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Integer codigoSucursal,
        Integer codigoPuntoVenta,
        String codigoRecepcion,
        String estado,
        String mensajeSiat,
        LocalDateTime createdAt
) {}
