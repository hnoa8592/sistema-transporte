package com.transporte.pasajes.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "reschedules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reschedule extends BaseEntity {

    @Column(name = "original_ticket_id", nullable = false)
    private UUID originalTicketId;

    @Column(name = "new_ticket_id")
    private UUID newTicketId;

    @Column(name = "new_schedule_id", nullable = false)
    private UUID newScheduleId;

    @Column(length = 500)
    private String reason;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "employee_id")
    private UUID employeeId;
}
