package com.transporte.operacion.entity;

import com.transporte.core.audit.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String dni;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "license_category", length = 10)
    private String licenseCategory;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
