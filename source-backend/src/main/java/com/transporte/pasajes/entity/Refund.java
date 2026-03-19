package com.transporte.pasajes.entity;

import com.transporte.core.audit.BaseEntity;
import com.transporte.pasajes.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund extends BaseEntity {

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(length = 500)
    private String reason;

    @Builder.Default
    @Column(name = "retention_percent", precision = 5, scale = 2)
    private BigDecimal retentionPercent = BigDecimal.ZERO;

    @Column(name = "original_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "retained_amount", precision = 10, scale = 2)
    private BigDecimal retainedAmount;

    @Column(name = "refunded_amount", precision = 10, scale = 2)
    private BigDecimal refundedAmount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(name = "employee_id")
    private UUID employeeId;
}
