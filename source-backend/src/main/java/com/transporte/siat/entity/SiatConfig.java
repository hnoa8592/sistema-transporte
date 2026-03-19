package com.transporte.siat.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "siat_config")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SiatConfig extends BaseEntity {

    @Column(name = "nit", nullable = false, length = 20)
    private String nit;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "codigo_sistema", nullable = false, length = 50)
    private String codigoSistema;

    @Column(name = "codigo_actividad", nullable = false, length = 10)
    private String codigoActividad;

    @Builder.Default
    @Column(name = "codigo_sucursal", nullable = false)
    private Integer codigoSucursal = 0;

    @Column(name = "codigo_punto_venta")
    private Integer codigoPuntoVenta;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Column(name = "municipio", length = 100)
    private String municipio;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Builder.Default
    @Column(name = "codigo_ambiente", nullable = false)
    private Integer codigoAmbiente = 2;

    @Builder.Default
    @Column(name = "codigo_modalidad", nullable = false)
    private Integer codigoModalidad = 2;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
