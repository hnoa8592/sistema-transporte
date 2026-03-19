package com.transporte.siat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SiatCuisResponse(
        UUID id,
        String cuis,
        LocalDateTime fechaVigencia,
        Integer codigoSucursal,
        Integer codigoPuntoVenta,
        Boolean activo,
        boolean vigente,
        LocalDateTime createdAt
) {}
