package com.transporte.operacion.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "buses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bus extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String plate;

    @Column(length = 100)
    private String model;

    @Column(length = 100)
    private String brand;

    @Column(name = "manufacture_year")
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fleet_id")
    private Fleet fleet;

    @Builder.Default
    @Column(name = "has_two_floors", nullable = false)
    private boolean hasTwoFloors = false;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "seats_first_floor")
    private Integer seatsFirstFloor;

    @Column(name = "seats_second_floor")
    private Integer seatsSecondFloor;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String notes;
}
