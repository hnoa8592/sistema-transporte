package com.transporte.pasajes.entity;

import com.transporte.core.audit.BaseEntity;
import com.transporte.pasajes.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "seat_maps")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMap extends BaseEntity {

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "travel_date", nullable = false)
    private java.time.LocalDate travelDate;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "ticket_id")
    private UUID ticketId;
}
