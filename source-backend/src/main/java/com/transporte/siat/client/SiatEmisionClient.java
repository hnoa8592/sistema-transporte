package com.transporte.siat.client;

import com.transporte.siat.config.SiatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

import javax.xml.transform.Source;
import java.util.Base64;

/**
 * Cliente SOAP para el ServicioFacturacionComputarizada del SIN Bolivia.
 * Operaciones: emisión individual, paquete, masiva, anulación, reversión.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiatEmisionClient {

    private static final String NS = "https://siat.impuestos.gob.bo/ServicioFacturacionComputarizada";

    private final WebServiceTemplate siatEmisionTemplate;
    private final SiatProperties siatProperties;
    private final SiatXmlParser xmlParser;

    // ==================== EMISIÓN INDIVIDUAL ====================

    /**
     * Envía una factura individual al SIN (recepcionFactura).
     *
     * @param nit             NIT del emisor
     * @param codigoSistema   Código de sistema registrado en el SIN
     * @param cuis            CUIS vigente
     * @param cufd            CUFD vigente
     * @param codigoSucursal  Código de sucursal
     * @param codigoPuntoVenta Código de punto de venta
     * @param xmlFirmado      XML de la factura firmado digitalmente (Base64)
     * @param cuf             CUF de la factura
     * @param fechaEnvio      Fecha de envío ISO
     * @param tipoEmision     1=Online
     */
    public SiatSoapResponse recepcionFactura(String nit, String codigoSistema,
                                              String cuis, String cufd,
                                              int codigoSucursal, Integer codigoPuntoVenta,
                                              String xmlFirmado, String cuf, String fechaEnvio,
                                              int tipoEmision) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String archivoBytesB64 = Base64.getEncoder().encodeToString(xmlFirmado.getBytes());
        String body = """
                <ser:recepcionFactura xmlns:ser="%s">
                  <SolicitudServicioRecepcionFactura>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoSucursal>%d</codigoSucursal>
                    <nit>%s</nit>
                    <codigoEmision>%d</codigoEmision>
                    <codigoModalidad>%d</codigoModalidad>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                    <codigoSistema>%s</codigoSistema>
                    <codigoTipoFactura>1</codigoTipoFactura>
                    <cuis>%s</cuis>
                    <cufd>%s</cufd>
                    <cuf>%s</cuf>
                    <archivoBytes>%s</archivoBytes>
                    <fechaEnvio>%s</fechaEnvio>
                  </SolicitudServicioRecepcionFactura>
                </ser:recepcionFactura>
                """.formatted(NS, siatProperties.getAmbiente(), codigoSucursal, nit,
                tipoEmision, siatProperties.getModalidad(), pvStr, codigoSistema,
                cuis, cufd, cuf, archivoBytesB64, fechaEnvio);

        return call(body, "recepcionFactura");
    }

    // ==================== PAQUETE ====================

    public SiatSoapResponse recepcionPaquete(String nit, String codigoSistema,
                                              String cuis, String cufd,
                                              int codigoSucursal, Integer codigoPuntoVenta,
                                              String archivoZipB64, int cantidadFacturas,
                                              String fechaEnvio, int tipoEmision) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String body = """
                <ser:recepcionPaqueteFactura xmlns:ser="%s">
                  <SolicitudServicioRecepcionPaquete>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoSucursal>%d</codigoSucursal>
                    <nit>%s</nit>
                    <codigoEmision>%d</codigoEmision>
                    <codigoModalidad>%d</codigoModalidad>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                    <codigoSistema>%s</codigoSistema>
                    <cuis>%s</cuis>
                    <cufd>%s</cufd>
                    <archivo>%s</archivo>
                    <fechaEnvio>%s</fechaEnvio>
                    <cantidadFacturas>%d</cantidadFacturas>
                  </SolicitudServicioRecepcionPaquete>
                </ser:recepcionPaqueteFactura>
                """.formatted(NS, siatProperties.getAmbiente(), codigoSucursal, nit,
                tipoEmision, siatProperties.getModalidad(), pvStr, codigoSistema,
                cuis, cufd, archivoZipB64, fechaEnvio, cantidadFacturas);

        return call(body, "recepcionPaqueteFactura");
    }

    public SiatSoapResponse validacionPaquete(String nit, String codigoSistema,
                                               String cuis, String cufd,
                                               int codigoSucursal, Integer codigoPuntoVenta,
                                               String codigoRecepcion) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String body = """
                <ser:validacionRecepcionPaqueteFactura xmlns:ser="%s">
                  <SolicitudServicioValidacionRecepcionPaquete>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoSucursal>%d</codigoSucursal>
                    <nit>%s</nit>
                    <codigoEmision>2</codigoEmision>
                    <codigoModalidad>%d</codigoModalidad>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                    <codigoSistema>%s</codigoSistema>
                    <cuis>%s</cuis>
                    <cufd>%s</cufd>
                    <codigoRecepcion>%s</codigoRecepcion>
                  </SolicitudServicioValidacionRecepcionPaquete>
                </ser:validacionRecepcionPaqueteFactura>
                """.formatted(NS, siatProperties.getAmbiente(), codigoSucursal, nit,
                siatProperties.getModalidad(), pvStr, codigoSistema, cuis, cufd, codigoRecepcion);

        return call(body, "validacionRecepcionPaqueteFactura");
    }

    // ==================== MASIVA ====================

    public SiatSoapResponse recepcionMasiva(String nit, String codigoSistema,
                                             String cuis, String cufd,
                                             int codigoSucursal, Integer codigoPuntoVenta,
                                             String archivoZipB64, int cantidadFacturas,
                                             String fechaEnvio) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String body = """
                <ser:recepcionMasivaFactura xmlns:ser="%s">
                  <SolicitudServicioRecepcionMasiva>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoSucursal>%d</codigoSucursal>
                    <nit>%s</nit>
                    <codigoModalidad>%d</codigoModalidad>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                    <codigoSistema>%s</codigoSistema>
                    <cuis>%s</cuis>
                    <cufd>%s</cufd>
                    <archivo>%s</archivo>
                    <fechaEnvio>%s</fechaEnvio>
                    <cantidadFacturas>%d</cantidadFacturas>
                  </SolicitudServicioRecepcionMasiva>
                </ser:recepcionMasivaFactura>
                """.formatted(NS, siatProperties.getAmbiente(), codigoSucursal, nit,
                siatProperties.getModalidad(), pvStr, codigoSistema, cuis, cufd,
                archivoZipB64, fechaEnvio, cantidadFacturas);

        return call(body, "recepcionMasivaFactura");
    }

    public SiatSoapResponse validacionMasiva(String nit, String codigoSistema,
                                              String cuis, String cufd,
                                              int codigoSucursal, Integer codigoPuntoVenta,
                                              String codigoRecepcion) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String body = """
                <ser:validacionRecepcionMasivaFactura xmlns:ser="%s">
                  <SolicitudServicioValidacionRecepcionMasiva>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoSucursal>%d</codigoSucursal>
                    <nit>%s</nit>
                    <codigoModalidad>%d</codigoModalidad>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                    <codigoSistema>%s</codigoSistema>
                    <cuis>%s</cuis>
                    <cufd>%s</cufd>
                    <codigoRecepcion>%s</codigoRecepcion>
                  </SolicitudServicioValidacionRecepcionMasiva>
                </ser:validacionRecepcionMasivaFactura>
                """.formatted(NS, siatProperties.getAmbiente(), codigoSucursal, nit,
                siatProperties.getModalidad(), pvStr, codigoSistema, cuis, cufd, codigoRecepcion);

        return call(body, "validacionRecepcionMasivaFactura");
    }

    // ==================== ANULACIÓN ====================

    public SiatSoapResponse anulacionFactura(String nit, String codigoSistema,
                                              String cuis, String cufd,
                                              int codigoSucursal, Integer codigoPuntoVenta,
                                              String cuf, int codigoMotivo) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String body = """
                <ser:anulacionFactura xmlns:ser="%s">
                  <SolicitudServicioAnulacionFactura>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoSucursal>%d</codigoSucursal>
                    <nit>%s</nit>
                    <codigoEmision>1</codigoEmision>
                    <codigoModalidad>%d</codigoModalidad>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                    <codigoSistema>%s</codigoSistema>
                    <cuis>%s</cuis>
                    <cufd>%s</cufd>
                    <cuf>%s</cuf>
                    <codigoMotivo>%d</codigoMotivo>
                  </SolicitudServicioAnulacionFactura>
                </ser:anulacionFactura>
                """.formatted(NS, siatProperties.getAmbiente(), codigoSucursal, nit,
                siatProperties.getModalidad(), pvStr, codigoSistema, cuis, cufd, cuf, codigoMotivo);

        return call(body, "anulacionFactura");
    }

    // ==================== REVERSIÓN ====================

    public SiatSoapResponse reversionAnulacion(String nit, String codigoSistema,
                                                String cuis, String cufd,
                                                int codigoSucursal, Integer codigoPuntoVenta,
                                                String cuf) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String body = """
                <ser:reversionAnulacionFactura xmlns:ser="%s">
                  <SolicitudServicioReversionAnulacionFactura>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoSucursal>%d</codigoSucursal>
                    <nit>%s</nit>
                    <codigoEmision>1</codigoEmision>
                    <codigoModalidad>%d</codigoModalidad>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                    <codigoSistema>%s</codigoSistema>
                    <cuis>%s</cuis>
                    <cufd>%s</cufd>
                    <cuf>%s</cuf>
                  </SolicitudServicioReversionAnulacionFactura>
                </ser:reversionAnulacionFactura>
                """.formatted(NS, siatProperties.getAmbiente(), codigoSucursal, nit,
                siatProperties.getModalidad(), pvStr, codigoSistema, cuis, cufd, cuf);

        return call(body, "reversionAnulacionFactura");
    }

    // ==================== VERIFICACIÓN ESTADO ====================

    public SiatSoapResponse verificarEstado(String nit, String codigoSistema,
                                             String cuis, String cufd,
                                             int codigoSucursal, Integer codigoPuntoVenta,
                                             String cuf) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String body = """
                <ser:verificacionEstadoFactura xmlns:ser="%s">
                  <SolicitudServicioVerificacionEstadoFactura>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoSucursal>%d</codigoSucursal>
                    <nit>%s</nit>
                    <codigoEmision>1</codigoEmision>
                    <codigoModalidad>%d</codigoModalidad>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                    <codigoSistema>%s</codigoSistema>
                    <cuis>%s</cuis>
                    <cufd>%s</cufd>
                    <cuf>%s</cuf>
                  </SolicitudServicioVerificacionEstadoFactura>
                </ser:verificacionEstadoFactura>
                """.formatted(NS, siatProperties.getAmbiente(), codigoSucursal, nit,
                siatProperties.getModalidad(), pvStr, codigoSistema, cuis, cufd, cuf);

        return call(body, "verificacionEstadoFactura");
    }

    // ==================== PRIVADO ====================

    private SiatSoapResponse call(String soapBody, String operation) {
        try {
            // sendSourceAndReceiveToResult envuelve automáticamente en SOAP envelope
            Source source = new StringSource(soapBody);
            StringResult result = new StringResult();
            siatEmisionTemplate.sendSourceAndReceiveToResult(source, result);
            String responseXml = result.toString();
            log.debug("SIAT {} response: {}", operation, responseXml);
            return xmlParser.parseResponse(responseXml);
        } catch (Exception e) {
            log.error("Error llamando SIAT {} : {}", operation, e.getMessage());
            return SiatSoapResponse.builder()
                    .exitoso(false)
                    .mensaje("Error de comunicación con el SIN: " + e.getMessage())
                    .datos(new java.util.HashMap<>())
                    .build();
        }
    }
}
