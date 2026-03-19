package com.transporte.usuarios.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resources")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(length = 100)
    private String module;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String description;
}
