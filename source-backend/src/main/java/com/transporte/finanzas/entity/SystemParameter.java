package com.transporte.finanzas.entity;

import com.transporte.core.audit.BaseEntity;
import com.transporte.finanzas.enums.ParameterType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_parameters")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemParameter extends BaseEntity {

    @Column(name = "param_key", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "param_value", nullable = false, length = 2000)
    private String value;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ParameterType type = ParameterType.STRING;

    @Column(length = 300)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
