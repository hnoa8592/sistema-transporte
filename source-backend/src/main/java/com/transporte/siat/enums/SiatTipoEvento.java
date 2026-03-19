package com.transporte.siat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SiatTipoEvento {
    CORTE_ENERGIA(1, "Corte de energía eléctrica"),
    DESCONEXION_INTERNET(2, "Desconexión o falla de internet"),
    FALLA_SISTEMA(3, "Falla en el sistema informático"),
    MOTIVOS_CLIMATICOS(4, "Causas por condiciones de adversidad climática"),
    AUTORIZACION_PREVIA(5, "Autorización previa por el SIN");

    private final int codigo;
    private final String descripcion;

    public static SiatTipoEvento fromCodigo(int codigo) {
        for (SiatTipoEvento t : values()) {
            if (t.codigo == codigo) return t;
        }
        throw new IllegalArgumentException("Tipo de evento SIAT desconocido: " + codigo);
    }
}
