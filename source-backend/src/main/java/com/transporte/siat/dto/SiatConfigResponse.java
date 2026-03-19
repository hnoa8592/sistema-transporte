package com.transporte.siat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SiatConfigResponse(
        UUID id,
        String nit,
        String razonSocial,
        String codigoSistema,
        String codigoActividad,
        Integer codigoSucursal,
        Integer codigoPuntoVenta,
        String direccion,
        String municipio,
        String telefono,
        Integer codigoAmbiente,
        Integer codigoModalidad,
        Boolean activo,
        LocalDateTime createdAt
) {}
