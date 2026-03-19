package com.transporte.encomiendas.repository;

import com.transporte.encomiendas.entity.Parcel;
import com.transporte.encomiendas.enums.ParcelStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, UUID> {
    Optional<Parcel> findByTrackingCode(String trackingCode);
    Page<Parcel> findAllByStatus(ParcelStatus status, Pageable pageable);
    Page<Parcel> findAll(Pageable pageable);
    boolean existsByTrackingCode(String trackingCode);
}
