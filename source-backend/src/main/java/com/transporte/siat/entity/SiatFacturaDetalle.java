package com.transporte.siat.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "siat_factura_detalle")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SiatFacturaDetalle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siat_factura_id", nullable = false)
    private SiatFactura siatFactura;

    @Column(name = "numero_linea", nullable = false)
    private Integer numeroLinea;

    @Column(name = "actividad_economica", length = 10)
    private String actividadEconomica;

    /** Código producto/servicio del SIN (p.ej. 84111 = servicios de transporte de pasajeros) */
    @Builder.Default
    @Column(name = "codigo_producto_sin", nullable = false)
    private Integer codigoProductoSin = 84111;

    @Column(name = "codigo_producto", length = 20)
    private String codigoProducto;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "cantidad", nullable = false, precision = 16, scale = 4)
    private BigDecimal cantidad;

    /** Código unidad de medida SIN (58 = Servicio) */
    @Builder.Default
    @Column(name = "unidad_medida", nullable = false)
    private Integer unidadMedida = 58;

    @Column(name = "precio_unitario", nullable = false, precision = 16, scale = 2)
    private BigDecimal precioUnitario;

    @Builder.Default
    @Column(name = "monto_descuento", nullable = false, precision = 16, scale = 2)
    private BigDecimal montoDescuento = BigDecimal.ZERO;

    @Column(name = "sub_total", nullable = false, precision = 16, scale = 2)
    private BigDecimal subTotal;
}
