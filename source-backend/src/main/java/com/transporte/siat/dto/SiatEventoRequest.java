package com.transporte.siat.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record SiatEventoRequest(
        @NotNull UUID siatConfigId,
        /** Código del evento (1=Corte energía, 2=Internet, 3=Falla sistema, 4=Clima, 5=Autorización) */
        @NotNull Integer codigoEvento,
        String descripcion,
        @NotNull LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Integer codigoSucursal,
        Integer codigoPuntoVenta
) {}
