package com.transporte.siat.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Parseador de respuestas XML del SIN Bolivia.
 * Extrae transEstado, transaccion, codigoRecepcion y mensajes de error.
 */
@Slf4j
@Component
public class SiatXmlParser {

    public SiatSoapResponse parseResponse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            Map<String, String> datos = new HashMap<>();
            datos.put("rawXml", xml); // para parseo de catálogos y listas
            boolean exitoso = false;
            String codigoRecepcion = null;
            String mensaje = null;

            // transEstado: true/false
            String transEstado = getTagValue(doc, "transEstado");
            if (transEstado != null) {
                exitoso = "true".equalsIgnoreCase(transEstado.trim());
                datos.put("transEstado", transEstado.trim());
            }

            // transaccion: true si la operación fue aceptada
            String transaccion = getTagValue(doc, "transaccion");
            if (transaccion != null) {
                exitoso = "true".equalsIgnoreCase(transaccion.trim());
                datos.put("transaccion", transaccion.trim());
            }

            // codigoRecepcion
            codigoRecepcion = getTagValue(doc, "codigoRecepcion");
            if (codigoRecepcion != null) datos.put("codigoRecepcion", codigoRecepcion.trim());

            // CUIS
            String cuis = getTagValue(doc, "codigo");
            if (cuis != null) datos.put("cuis", cuis.trim());

            // CUFD específico
            String cufd = getTagValue(doc, "cufd");
            if (cufd != null) datos.put("cufd", cufd.trim());

            String codigoControl = getTagValue(doc, "codigoControl");
            if (codigoControl != null) datos.put("codigoControl", codigoControl.trim());

            String codigoParaQr = getTagValue(doc, "codigoParaQr");
            if (codigoParaQr != null) datos.put("codigoParaQr", codigoParaQr.trim());

            String fechaVigencia = getTagValue(doc, "fechaVigencia");
            if (fechaVigencia != null) datos.put("fechaVigencia", fechaVigencia.trim());

            // Mensajes de error/descripción
            String descripcion = getTagValue(doc, "descripcion");
            if (descripcion != null) {
                mensaje = descripcion.trim();
                datos.put("descripcion", descripcion.trim());
            }

            String codigoDescripcion = getTagValue(doc, "codigoDescripcion");
            if (codigoDescripcion != null) datos.put("codigoDescripcion", codigoDescripcion.trim());

            // mensajesList
            NodeList mensajes = doc.getElementsByTagNameNS("*", "mensajesList");
            if (mensajes.getLength() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mensajes.getLength(); i++) {
                    String m = mensajes.item(i).getTextContent();
                    if (m != null && !m.isBlank()) sb.append(m.trim()).append("; ");
                }
                if (!sb.isEmpty()) {
                    mensaje = sb.toString();
                    datos.put("mensajes", mensaje);
                }
            }

            // Estado validación de paquete
            String estadoValidacion = getTagValue(doc, "estadoSolicitud");
            if (estadoValidacion != null) datos.put("estadoValidacion", estadoValidacion.trim());

            return SiatSoapResponse.builder()
                    .exitoso(exitoso)
                    .codigoRecepcion(codigoRecepcion != null ? codigoRecepcion.trim() : null)
                    .mensaje(mensaje)
                    .datos(datos)
                    .build();

        } catch (Exception e) {
            log.error("Error parseando respuesta SIAT: {}", e.getMessage());
            return SiatSoapResponse.builder()
                    .exitoso(false)
                    .mensaje("Error parseando respuesta: " + e.getMessage())
                    .datos(Map.of())
                    .build();
        }
    }

    private String getTagValue(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagNameNS("*", tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return null;
    }
}
