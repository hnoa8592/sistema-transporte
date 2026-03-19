package com.transporte.siat.client;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Respuesta genérica de las operaciones SOAP del SIN.
 */
@Getter
@Builder
public class SiatSoapResponse {

    /** true si el SIN aceptó la operación */
    private final boolean exitoso;

    /** Código de transacción/recepción del SIN */
    private final String codigoRecepcion;

    /** Mensaje descriptivo del SIN */
    private final String mensaje;

    /** Valores adicionales extraídos de la respuesta XML */
    private final Map<String, String> datos;
}
