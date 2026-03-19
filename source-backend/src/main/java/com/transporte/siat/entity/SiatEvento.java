package com.transporte.siat.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "siat_evento")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SiatEvento extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siat_config_id", nullable = false)
    private SiatConfig siatConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siat_cufd_id", nullable = false)
    private SiatCufd siatCufd;

    @Column(name = "codigo_evento", nullable = false)
    private Integer codigoEvento;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Builder.Default
    @Column(name = "codigo_sucursal", nullable = false)
    private Integer codigoSucursal = 0;

    @Column(name = "codigo_punto_venta")
    private Integer codigoPuntoVenta;

    @Column(name = "codigo_recepcion", length = 100)
    private String codigoRecepcion;

    @Builder.Default
    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "mensaje_siat", length = 500)
    private String mensajeSiat;
}
