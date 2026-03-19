package com.transporte.siat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.siat")
public class SiatProperties {

    /** URL del servicio SOAP de códigos (CUIS, CUFD, catálogos, eventos) */
    private String endpointCodigos = "https://siatrest.impuestos.gob.bo/v2/ServicioFacturacionCodigos";

    /** URL del servicio SOAP de facturación (emisión, anulación, reversión) */
    private String endpointFacturacion = "https://siatrest.impuestos.gob.bo/v2/ServicioFacturacionComputarizada";

    /** Código de ambiente: 1=Producción, 2=Pruebas */
    private int ambiente = 2;

    /** Código de modalidad: 1=Electrónica, 2=Computarizada */
    private int modalidad = 2;

    /** Ruta al certificado digital PKCS12 (.p12 / .pfx) */
    private String certificatePath;

    /** Contraseña del certificado digital */
    private String certificatePassword;

    /** Timeout de conexión SOAP en milisegundos */
    private int connectionTimeout = 5000;

    /** Timeout de lectura SOAP en milisegundos */
    private int readTimeout = 60000;
}
