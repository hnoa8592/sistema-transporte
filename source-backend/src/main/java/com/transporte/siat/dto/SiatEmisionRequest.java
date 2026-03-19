package com.transporte.siat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SiatEmisionRequest(
        /** ID de la factura interna (opcional, para vinculación) */
        UUID invoiceId,

        @NotNull UUID siatConfigId,

        /** Nombre o razón social del receptor */
        @NotBlank String nombreRazonSocial,

        /** 1=CI, 2=CEX, 3=PAS, 4=OD, 5=NIT */
        @NotNull Integer codigoTipoDocumentoIdentidad,

        @NotBlank String numeroDocumento,
        String complemento,
        String codigoCliente,

        /** 1=Efectivo, 2=Tarjeta, 3=Cheque, etc. */
        Integer codigoMetodoPago,

        /** Código de moneda (1=BOB) */
        Integer codigoMoneda,

        BigDecimal tipoCambio,

        /** 1=Online, 2=Fuera de línea (offline) */
        Integer tipoEmision,

        @NotEmpty @Valid List<SiatEmisionDetalleRequest> detalles
) {}
