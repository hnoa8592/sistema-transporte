package com.transporte.siat.dto;

import com.transporte.siat.enums.SiatEstadoEmision;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SiatEmisionResponse(
        UUID id,
        UUID invoiceId,
        String cuf,
        Long numeroFactura,
        String nitEmisor,
        String razonSocialEmisor,
        String nombreRazonSocial,
        Integer codigoTipoDocumentoIdentidad,
        String numeroDocumento,
        BigDecimal importeTotal,
        BigDecimal importeTotalSujetoIva,
        BigDecimal tipoCambio,
        Integer codigoMoneda,
        Integer codigoMetodoPago,
        String codigoActividad,
        SiatEstadoEmision estadoEmision,
        Integer tipoEmision,
        String codigoRecepcion,
        String mensajeSiat,
        String qrContent,
        LocalDateTime fechaEmision,
        List<SiatEmisionDetalleResponse> detalles,
        LocalDateTime createdAt
) {}
