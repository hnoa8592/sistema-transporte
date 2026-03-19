package com.transporte.siat.dto;

import com.transporte.siat.enums.SiatTipoCatalogo;

import java.util.UUID;

public record SiatCatalogoResponse(
        UUID id,
        SiatTipoCatalogo tipoCatalogo,
        String codigo,
        String descripcion,
        Boolean vigente
) {}
