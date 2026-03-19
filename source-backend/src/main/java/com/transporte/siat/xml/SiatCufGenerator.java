package com.transporte.siat.xml;

import org.apache.commons.codec.binary.Base16;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generador del CUF (Código Único de Factura) según especificación SIAT Bolivia.
 * <p>
 * Formato: NIT + FechaHora(14) + Sucursal(4) + Modalidad(2) + TipoDocFiscal(2)
 *          + TipoEmision(1) + NumeroFactura(10) + POS(6) + CUFD(10chars) → SHA256 → Base16
 */
@Component
public class SiatCufGenerator {

    private static final DateTimeFormatter FECHA_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Genera el CUF para una factura.
     *
     * @param nit              NIT del emisor
     * @param fechaEmision     Fecha y hora de emisión
     * @param codigoSucursal   Código de sucursal (4 dígitos con ceros)
     * @param codigoModalidad  1=Electrónica, 2=Computarizada
     * @param tipoDocFiscal    Código de tipo de documento fiscal (ej. 1)
     * @param tipoEmision      1=Online, 2=Offline
     * @param numeroFactura    Número correlativo de la factura
     * @param codigoPOS        Código de punto de venta (6 dígitos con ceros)
     * @param cufd             CUFD vigente
     * @return CUF en Base16 (hexadecimal en mayúsculas)
     */
    public String generate(String nit, LocalDateTime fechaEmision, int codigoSucursal,
                           int codigoModalidad, int tipoDocFiscal, int tipoEmision,
                           long numeroFactura, int codigoPOS, String cufd) {
        try {
            String fechaStr = fechaEmision.format(FECHA_FORMAT);
            String sucursalStr = String.format("%04d", codigoSucursal);
            String modalidadStr = String.format("%02d", codigoModalidad);
            String tipoDocStr = String.format("%02d", tipoDocFiscal);
            String tipoEmisionStr = String.valueOf(tipoEmision);
            String numFacturaStr = String.format("%010d", numeroFactura);
            String posStr = String.format("%06d", codigoPOS);
            // Tomar los primeros 10 caracteres del CUFD para el hash
            String cufdPart = cufd.length() >= 10 ? cufd.substring(0, 10) : cufd;

            String input = nit + fechaStr + sucursalStr + modalidadStr + tipoDocStr
                    + tipoEmisionStr + numFacturaStr + posStr + cufdPart;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            Base16 base16 = new Base16(false); // uppercase
            return base16.encodeToString(hash).toUpperCase();
        } catch (Exception e) {
            throw new IllegalStateException("Error generando CUF: " + e.getMessage(), e);
        }
    }

    /**
     * Genera el contenido del QR para la factura.
     * Formato: nit|fechaEmision|importeTotal|numeroFactura|codigoAutorizacion(CUFD)|cuf|leyenda|codigoControl
     */
    public String generateQrContent(String nit, LocalDateTime fechaEmision, BigDecimal importeTotal,
                                    long numeroFactura, String cufd, String cuf,
                                    String leyenda, String codigoControl) {
        return String.join("|",
                nit,
                fechaEmision.format(FECHA_FORMAT),
                importeTotal.toPlainString(),
                String.valueOf(numeroFactura),
                cufd,
                cuf,
                leyenda != null ? leyenda : "",
                codigoControl != null ? codigoControl : ""
        );
    }
}
