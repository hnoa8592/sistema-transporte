package com.transporte.siat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SiatAmbiente {
    PRODUCCION(1, "Producción"),
    PRUEBAS(2, "Pruebas");

    private final int codigo;
    private final String descripcion;

    public static SiatAmbiente fromCodigo(int codigo) {
        for (SiatAmbiente a : values()) {
            if (a.codigo == codigo) return a;
        }
        throw new IllegalArgumentException("Ambiente SIAT desconocido: " + codigo);
    }
}
