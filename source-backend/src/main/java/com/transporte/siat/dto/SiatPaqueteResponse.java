package com.transporte.siat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SiatPaqueteResponse(
        UUID id,
        Integer codigoSucursal,
        Integer codigoPuntoVenta,
        Integer cantidadFacturas,
        Integer tipoEmision,
        String codigoRecepcion,
        String estado,
        String estadoValidacion,
        String mensajeSiat,
        LocalDateTime fechaEmision,
        LocalDateTime createdAt
) {}
