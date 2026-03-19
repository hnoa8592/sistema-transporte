package com.transporte.siat.entity;

import com.transporte.core.audit.BaseEntity;
import com.transporte.siat.enums.SiatTipoCatalogo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "siat_catalogo")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SiatCatalogo extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_catalogo", nullable = false, length = 60)
    private SiatTipoCatalogo tipoCatalogo;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String descripcion;

    @Builder.Default
    @Column(name = "vigente", nullable = false)
    private Boolean vigente = true;

    /** JSON extra: para productos puede contener codigoActividad, etc. */
    @Column(name = "datos_extra", columnDefinition = "jsonb")
    private String datosExtra;
}
