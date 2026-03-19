package com.transporte.encomiendas.entity;

import com.transporte.core.audit.BaseEntity;
import com.transporte.encomiendas.enums.ParcelStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "parcels")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Parcel extends BaseEntity {

    @Column(name = "tracking_code", unique = true, nullable = false, length = 50)
    private String trackingCode;

    @Column(name = "sender_id")
    private UUID senderId;

    @Column(name = "sender_name", length = 200)
    private String senderName;

    @Column(name = "sender_phone", length = 20)
    private String senderPhone;

    @Column(name = "recipient_id")
    private UUID recipientId;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "schedule_id")
    private UUID scheduleId;

    @Column(length = 500)
    private String description;

    @Column(precision = 10, scale = 3)
    private BigDecimal weight;

    @Column(name = "declared_value", precision = 10, scale = 2)
    private BigDecimal declaredValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParcelStatus status = ParcelStatus.RECIBIDO;

    @Column(name = "employee_id")
    private UUID employeeId;
}
