package com.transporte.operacion.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "terminal_lanes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalLane extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id", nullable = false)
    private Terminal terminal;

    @Column(name = "lane_number", nullable = false)
    private int number;

    @Column(length = 200)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
