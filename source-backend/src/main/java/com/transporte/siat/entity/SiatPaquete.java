package com.transporte.siat.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "siat_paquete")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SiatPaquete extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siat_config_id", nullable = false)
    private SiatConfig siatConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siat_cufd_id", nullable = false)
    private SiatCufd siatCufd;

    @Builder.Default
    @Column(name = "codigo_sucursal", nullable = false)
    private Integer codigoSucursal = 0;

    @Column(name = "codigo_punto_venta")
    private Integer codigoPuntoVenta;

    @Builder.Default
    @Column(name = "cantidad_facturas", nullable = false)
    private Integer cantidadFacturas = 0;

    /** 1=Online, 2=Fuera de línea */
    @Builder.Default
    @Column(name = "tipo_emision", nullable = false)
    private Integer tipoEmision = 2;

    @Column(name = "codigo_recepcion", length = 100)
    private String codigoRecepcion;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "estado_validacion", length = 30)
    private String estadoValidacion;

    @Column(name = "mensaje_siat", length = 500)
    private String mensajeSiat;

    @Column(name = "archivo_zip", columnDefinition = "TEXT")
    private String archivoZip;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;
}
