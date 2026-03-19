package com.transporte.operacion.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "terminals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Terminal extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "terminal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TerminalLane> lanes = new ArrayList<>();
}
