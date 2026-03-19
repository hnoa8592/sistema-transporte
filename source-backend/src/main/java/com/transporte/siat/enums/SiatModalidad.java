package com.transporte.siat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SiatModalidad {
    ELECTRONICA(1, "Electrónica en Línea"),
    COMPUTARIZADA(2, "Computarizada en Línea");

    private final int codigo;
    private final String descripcion;
}
