package com.transporte.siat.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "siat_cufd")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SiatCufd extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siat_config_id", nullable = false)
    private SiatConfig siatConfig;

    @Column(name = "cufd", nullable = false, length = 200)
    private String cufd;

    @Column(name = "codigo_control", nullable = false, length = 50)
    private String codigoControl;

    @Column(name = "codigo_para_qr", length = 300)
    private String codigoParaQr;

    @Column(name = "fecha_vigencia", nullable = false)
    private LocalDateTime fechaVigencia;

    @Builder.Default
    @Column(name = "codigo_sucursal", nullable = false)
    private Integer codigoSucursal = 0;

    @Column(name = "codigo_punto_venta")
    private Integer codigoPuntoVenta;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public boolean isVigente() {
        return activo && fechaVigencia != null && LocalDateTime.now().isBefore(fechaVigencia);
    }
}
