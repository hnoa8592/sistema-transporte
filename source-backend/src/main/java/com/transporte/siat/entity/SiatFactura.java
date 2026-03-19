package com.transporte.siat.entity;

import com.transporte.core.audit.BaseEntity;
import com.transporte.siat.enums.SiatEstadoEmision;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "siat_factura")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SiatFactura extends BaseEntity {

    /** Referencia a la factura interna del sistema */
    @Column(name = "invoice_id")
    private UUID invoiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siat_config_id", nullable = false)
    private SiatConfig siatConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siat_cufd_id", nullable = false)
    private SiatCufd siatCufd;

    /** CUF: Código Único de Factura (generado localmente) */
    @Column(name = "cuf", unique = true, length = 100)
    private String cuf;

    @Column(name = "numero_factura", nullable = false)
    private Long numeroFactura;

    @Builder.Default
    @Column(name = "codigo_sucursal", nullable = false)
    private Integer codigoSucursal = 0;

    @Column(name = "codigo_punto_venta")
    private Integer codigoPuntoVenta;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    // --- Emisor ---
    @Column(name = "nit_emisor", nullable = false, length = 20)
    private String nitEmisor;

    @Column(name = "razon_social_emisor", length = 200)
    private String razonSocialEmisor;

    // --- Receptor ---
    @Column(name = "nombre_razon_social", length = 200)
    private String nombreRazonSocial;

    @Builder.Default
    @Column(name = "codigo_tipo_documento_identidad", nullable = false)
    private Integer codigoTipoDocumentoIdentidad = 1;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(name = "complemento", length = 10)
    private String complemento;

    @Column(name = "codigo_cliente", length = 50)
    private String codigoCliente;

    // --- Importes ---
    @Builder.Default
    @Column(name = "importe_total_sujeto_iva", nullable = false, precision = 16, scale = 2)
    private BigDecimal importeTotalSujetoIva = BigDecimal.ZERO;

    @Column(name = "importe_total", nullable = false, precision = 16, scale = 2)
    private BigDecimal importeTotal;

    @Builder.Default
    @Column(name = "tipo_cambio", nullable = false, precision = 10, scale = 4)
    private BigDecimal tipoCambio = BigDecimal.ONE;

    @Builder.Default
    @Column(name = "codigo_moneda", nullable = false)
    private Integer codigoMoneda = 1; // 1 = BOB

    @Builder.Default
    @Column(name = "codigo_metodo_pago", nullable = false)
    private Integer codigoMetodoPago = 1; // 1 = Efectivo

    // --- Clasificación ---
    @Column(name = "codigo_actividad", length = 10)
    private String codigoActividad;

    @Builder.Default
    @Column(name = "codigo_documento_sector", nullable = false)
    private Integer codigoDocumentoSector = 1;

    // --- Estado SIAT ---
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_emision", nullable = false, length = 30)
    private SiatEstadoEmision estadoEmision = SiatEstadoEmision.PENDIENTE;

    @Builder.Default
    @Column(name = "tipo_emision", nullable = false)
    private Integer tipoEmision = 1; // 1=Online, 2=Offline

    @Column(name = "codigo_recepcion", length = 100)
    private String codigoRecepcion;

    @Column(name = "mensaje_siat", length = 500)
    private String mensajeSiat;

    // --- XML / QR ---
    @Column(name = "xml_content", columnDefinition = "TEXT")
    private String xmlContent;

    @Column(name = "xml_firmado", columnDefinition = "TEXT")
    private String xmlFirmado;

    @Column(name = "qr_content", columnDefinition = "TEXT")
    private String qrContent;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    @Column(name = "siat_paquete_id")
    private UUID siatPaqueteId;

    @OneToMany(mappedBy = "siatFactura", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SiatFacturaDetalle> detalles = new ArrayList<>();
}
