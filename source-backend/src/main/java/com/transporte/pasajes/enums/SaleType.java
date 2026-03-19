package com.transporte.pasajes.enums;

public enum SaleType {
    /** Venta presencial en ventanilla (equivalente a COUNTER) */
    VENTANILLA,
    /** Alias REST-friendly de VENTANILLA usado desde el frontend */
    COUNTER,
    /** Venta en línea */
    ONLINE,
    /** Venta a través de agencia */
    AGENCY
}
