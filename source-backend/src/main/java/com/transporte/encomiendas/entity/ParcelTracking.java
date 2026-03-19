package com.transporte.encomiendas.entity;

import com.transporte.core.audit.BaseEntity;
import com.transporte.encomiendas.enums.ParcelStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "parcel_trackings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelTracking extends BaseEntity {

    @Column(name = "parcel_id", nullable = false)
    private UUID parcelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParcelStatus status;

    @Column(length = 200)
    private String location;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 500)
    private String notes;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
