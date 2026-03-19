package com.transporte.siat.client;

import com.transporte.siat.config.SiatProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;

import javax.xml.transform.Source;

/**
 * Cliente SOAP para el ServicioFacturacionCodigos del SIN Bolivia.
 * Operaciones: CUIS, CUFD, sincronización de catálogos, eventos significativos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiatCodesClient {

    private static final String NS = "https://siat.impuestos.gob.bo/";
    private static final String WSDL_CODIGOS = "https://siatrest.impuestos.gob.bo/v2/ServicioFacturacionCodigos?wsdl";

    private final WebServiceTemplate siatCodesTemplate;
    private final SiatProperties siatProperties;
    private final SiatXmlParser xmlParser;

    // ==================== CUIS ====================

    public SiatSoapResponse obtenerCuis(String nit, String codigoSistema,
                                        int codigoSucursal, Integer codigoPuntoVenta) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String body = """
                <ser:obtenerCuis xmlns:ser="%s">
                  <SolicitudCuis>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoModalidad>%d</codigoModalidad>
                    <codigoSucursal>%d</codigoSucursal>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                    <nit>%s</nit>
                    <codigoSistema>%s</codigoSistema>
                  </SolicitudCuis>
                </ser:obtenerCuis>
                """.formatted(NS, siatProperties.getAmbiente(), siatProperties.getModalidad(),
                codigoSucursal, pvStr, nit, codigoSistema);

        return call(body, "obtenerCuis");
    }

    // ==================== CUFD ====================

    public SiatSoapResponse obtenerCufd(String nit, String codigoSistema,
                                        String cuis, int codigoSucursal, Integer codigoPuntoVenta) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String body = """
                <ser:obtenerCufd xmlns:ser="%s">
                  <SolicitudCufd>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoModalidad>%d</codigoModalidad>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                    <nit>%s</nit>
                    <codigoSucursal>%d</codigoSucursal>
                    <cuis>%s</cuis>
                  </SolicitudCufd>
                </ser:obtenerCufd>
                """.formatted(NS, siatProperties.getAmbiente(), siatProperties.getModalidad(),
                pvStr, nit, codigoSucursal, cuis);

        return call(body, "obtenerCufd");
    }

    // ==================== CATÁLOGOS ====================

    public SiatSoapResponse sincronizarActividades(String nit, String codigoSistema, String cuis,
                                                    int codigoSucursal, Integer codigoPuntoVenta) {
        return callCatalogo("sincronizarActividades", nit, codigoSistema, cuis, codigoSucursal, codigoPuntoVenta);
    }

    public SiatSoapResponse sincronizarProductosServicios(String nit, String codigoSistema, String cuis,
                                                           int codigoSucursal, Integer codigoPuntoVenta) {
        return callCatalogo("sincronizarListaProductosServicios", nit, codigoSistema, cuis, codigoSucursal, codigoPuntoVenta);
    }

    public SiatSoapResponse sincronizarLeyendas(String nit, String codigoSistema, String cuis,
                                                 int codigoSucursal, Integer codigoPuntoVenta) {
        return callCatalogo("sincronizarListaLeyendasFactura", nit, codigoSistema, cuis, codigoSucursal, codigoPuntoVenta);
    }

    public SiatSoapResponse sincronizarTiposDocumentoIdentidad(String nit, String codigoSistema, String cuis,
                                                                int codigoSucursal, Integer codigoPuntoVenta) {
        return callCatalogo("sincronizarParametricaTipoDocumentoIdentidad",
                nit, codigoSistema, cuis, codigoSucursal, codigoPuntoVenta);
    }

    public SiatSoapResponse sincronizarMetodosPago(String nit, String codigoSistema, String cuis,
                                                    int codigoSucursal, Integer codigoPuntoVenta) {
        return callCatalogo("sincronizarParametricaTipoMetodoPago",
                nit, codigoSistema, cuis, codigoSucursal, codigoPuntoVenta);
    }

    public SiatSoapResponse sincronizarMonedas(String nit, String codigoSistema, String cuis,
                                                int codigoSucursal, Integer codigoPuntoVenta) {
        return callCatalogo("sincronizarParametricaMoneda",
                nit, codigoSistema, cuis, codigoSucursal, codigoPuntoVenta);
    }

    public SiatSoapResponse sincronizarUnidadesMedida(String nit, String codigoSistema, String cuis,
                                                       int codigoSucursal, Integer codigoPuntoVenta) {
        return callCatalogo("sincronizarListaUnidadMedida",
                nit, codigoSistema, cuis, codigoSucursal, codigoPuntoVenta);
    }

    // ==================== EVENTOS SIGNIFICATIVOS ====================

    public SiatSoapResponse registrarEvento(String nit, String codigoSistema, String cuis,
                                             int codigoSucursal, Integer codigoPuntoVenta,
                                             int codigoEvento, String descripcion,
                                             String fechaInicio, String fechaFin) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String body = """
                <ser:registroEventoSignificativo xmlns:ser="%s">
                  <SolicitudEventoSignificativo>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoSistema>%s</codigoSistema>
                    <nit>%s</nit>
                    <cuis>%s</cuis>
                    <codigoSucursal>%d</codigoSucursal>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                    <codigoEvento>%d</codigoEvento>
                    <descripcion>%s</descripcion>
                    <fechaHoraInicioEvento>%s</fechaHoraInicioEvento>
                    <fechaHoraFinEvento>%s</fechaHoraFinEvento>
                  </SolicitudEventoSignificativo>
                </ser:registroEventoSignificativo>
                """.formatted(NS, siatProperties.getAmbiente(), codigoSistema, nit,
                cuis, codigoSucursal, pvStr, codigoEvento, descripcion, fechaInicio,
                fechaFin != null ? fechaFin : "");

        return call(body, "registroEventoSignificativo");
    }

    // ==================== COMUNICACIÓN ====================

    public SiatSoapResponse verificarComunicacion() {
        String body = """
                <ser:verificarComunicacion xmlns:ser="%s">
                  <SolicitudVerificarComunicacion/>
                </ser:verificarComunicacion>
                """.formatted(NS);
        return call(body, "verificarComunicacion");
    }

    // ==================== PRIVADO ====================

    private SiatSoapResponse callCatalogo(String operation, String nit, String codigoSistema,
                                           String cuis, int codigoSucursal, Integer codigoPuntoVenta) {
        String pvStr = codigoPuntoVenta != null ? String.valueOf(codigoPuntoVenta) : "0";
        String body = """
                <ser:%s xmlns:ser="%s">
                  <SolicitudSincronizacion>
                    <codigoAmbiente>%d</codigoAmbiente>
                    <codigoSistema>%s</codigoSistema>
                    <nit>%s</nit>
                    <cuis>%s</cuis>
                    <codigoSucursal>%d</codigoSucursal>
                    <codigoPuntoVenta>%s</codigoPuntoVenta>
                  </SolicitudSincronizacion>
                </%s>
                """.formatted(operation, NS, siatProperties.getAmbiente(), codigoSistema,
                nit, cuis, codigoSucursal, pvStr, "ser:" + operation);

        return call(body, operation);
    }

    private SiatSoapResponse call(String soapBody, String operation) {
        try {
            // sendSourceAndReceiveToResult envuelve automáticamente en SOAP envelope
            Source source = new StringSource(soapBody);
            StringResult result = new StringResult();
            siatCodesTemplate.sendSourceAndReceiveToResult(source, result);
            String responseXml = result.toString();
            log.debug("SIAT {} response: {}", operation, responseXml);
            return xmlParser.parseResponse(responseXml);
        } catch (Exception e) {
            log.error("Error llamando SIAT {} : {}", operation, e.getMessage());
            return SiatSoapResponse.builder()
                    .exitoso(false)
                    .mensaje("Error de comunicación: " + e.getMessage())
                    .datos(new java.util.HashMap<>())
                    .build();
        }
    }
}
