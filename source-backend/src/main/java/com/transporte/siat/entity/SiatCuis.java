package com.transporte.siat.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "siat_cuis")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SiatCuis extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siat_config_id", nullable = false)
    private SiatConfig siatConfig;

    @Column(name = "cuis", nullable = false, length = 100)
    private String cuis;

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
