package com.transporte.siat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SiatCufdResponse(
        UUID id,
        String cufd,
        String codigoControl,
        String codigoParaQr,
        LocalDateTime fechaVigencia,
        Integer codigoSucursal,
        Integer codigoPuntoVenta,
        Boolean activo,
        boolean vigente,
        LocalDateTime createdAt
) {}
