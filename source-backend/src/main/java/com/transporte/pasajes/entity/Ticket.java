package com.transporte.pasajes.entity;

import com.transporte.core.audit.BaseEntity;
import com.transporte.pasajes.enums.SaleType;
import com.transporte.pasajes.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket extends BaseEntity {

    @Column(name = "ticket_code", unique = true, nullable = false, length = 50)
    private String ticketCode;

    @Column(name = "schedule_id", nullable = false)
    private UUID scheduleId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 20)
    private SaleType saleType = SaleType.VENTANILLA;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "passenger_name", length = 200)
    private String passengerName;

    @Column(name = "passenger_document", length = 20)
    private String passengerDocument;
}
