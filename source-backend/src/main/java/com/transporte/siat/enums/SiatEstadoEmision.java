package com.transporte.siat.enums;

public enum SiatEstadoEmision {
    /** Factura generada localmente, aún no enviada al SIN */
    PENDIENTE,
    /** Enviada al SIN, esperando validación */
    ENVIADO,
    /** Aceptada y válida por el SIN */
    VALIDO,
    /** Observada con errores corregibles */
    OBSERVADO,
    /** Rechazada por el SIN */
    RECHAZADO,
    /** Anulada en el SIN */
    ANULADO,
    /** Anulación revertida */
    REVERTIDO,
    /** En paquete de emisión offline */
    EN_PAQUETE
}
