package com.transporte.siat.xml;

import com.transporte.siat.entity.SiatFactura;
import com.transporte.siat.entity.SiatFacturaDetalle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Constructor del XML de factura SIAT Bolivia.
 * Genera el XML conforme al esquema FacturaComercialesCompraVenta v1.0 del SIN.
 */
@Component
public class SiatXmlBuilder {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public String buildFacturaXml(SiatFactura factura) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<facturaComercialExportacion xmlns=\"urn:siat:facturaComercialExportacion:v1\">\n");
        sb.append("  <cabecera>\n");
        appendTag(sb, "nitEmisor", factura.getNitEmisor(), 4);
        appendTag(sb, "razonSocialEmisor", xml(factura.getRazonSocialEmisor()), 4);
        appendTag(sb, "municipio", xml(factura.getSiatConfig().getMunicipio()), 4);
        appendTag(sb, "telefono", factura.getSiatConfig().getTelefono(), 4);
        appendTag(sb, "numeroFactura", String.valueOf(factura.getNumeroFactura()), 4);
        appendTag(sb, "cuf", factura.getCuf(), 4);
        appendTag(sb, "cufd", factura.getSiatCufd().getCufd(), 4);
        appendTag(sb, "codigoSucursal", String.valueOf(factura.getCodigoSucursal()), 4);
        appendTag(sb, "direccion", xml(factura.getSiatConfig().getDireccion()), 4);
        appendTag(sb, "codigoPuntoVenta",
                factura.getCodigoPuntoVenta() != null ? String.valueOf(factura.getCodigoPuntoVenta()) : "0", 4);
        appendTag(sb, "fechaEmision", factura.getFechaEmision().format(ISO_FORMAT), 4);
        appendTag(sb, "nombreRazonSocial", xml(factura.getNombreRazonSocial()), 4);
        appendTag(sb, "codigoTipoDocumentoIdentidad",
                String.valueOf(factura.getCodigoTipoDocumentoIdentidad()), 4);
        appendTag(sb, "numeroDocumento", factura.getNumeroDocumento(), 4);
        if (factura.getComplemento() != null) {
            appendTag(sb, "complemento", factura.getComplemento(), 4);
        }
        appendTag(sb, "codigoCliente",
                factura.getCodigoCliente() != null ? factura.getCodigoCliente() : factura.getNumeroDocumento(), 4);
        appendTag(sb, "codigoMetodoPago", String.valueOf(factura.getCodigoMetodoPago()), 4);
        appendTag(sb, "importeTotalSujetoIva",
                formatAmount(factura.getImporteTotalSujetoIva()), 4);
        appendTag(sb, "codigoMoneda", String.valueOf(factura.getCodigoMoneda()), 4);
        appendTag(sb, "tipoCambio", factura.getTipoCambio().toPlainString(), 4);
        appendTag(sb, "importeTotal", formatAmount(factura.getImporteTotal()), 4);
        appendTag(sb, "codigoDocumentoSector", String.valueOf(factura.getCodigoDocumentoSector()), 4);
        appendTag(sb, "codigoActividad",
                factura.getCodigoActividad() != null
                        ? factura.getCodigoActividad()
                        : factura.getSiatConfig().getCodigoActividad(), 4);
        appendTag(sb, "usuario", factura.getCreatedBy() != null ? factura.getCreatedBy() : "sistema", 4);
        sb.append("  </cabecera>\n");

        int linea = 1;
        for (SiatFacturaDetalle d : factura.getDetalles()) {
            sb.append("  <detalle>\n");
            appendTag(sb, "actividadEconomica",
                    d.getActividadEconomica() != null
                            ? d.getActividadEconomica()
                            : factura.getSiatConfig().getCodigoActividad(), 4);
            appendTag(sb, "codigoProductoSin", String.valueOf(d.getCodigoProductoSin()), 4);
            if (d.getCodigoProducto() != null) {
                appendTag(sb, "codigoProducto", d.getCodigoProducto(), 4);
            }
            appendTag(sb, "descripcion", xml(d.getDescripcion()), 4);
            appendTag(sb, "cantidad", formatAmount(d.getCantidad()), 4);
            appendTag(sb, "unidadMedida", String.valueOf(d.getUnidadMedida()), 4);
            appendTag(sb, "precioUnitario", formatAmount(d.getPrecioUnitario()), 4);
            appendTag(sb, "montoDescuento", formatAmount(d.getMontoDescuento()), 4);
            appendTag(sb, "subTotal", formatAmount(d.getSubTotal()), 4);
            sb.append("  </detalle>\n");
            linea++;
        }

        sb.append("</facturaComercialExportacion>\n");
        return sb.toString();
    }

    private void appendTag(StringBuilder sb, String tag, String value, int indent) {
        sb.append(" ".repeat(indent))
                .append("<").append(tag).append(">")
                .append(value != null ? value : "")
                .append("</").append(tag).append(">\n");
    }

    private String xml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
