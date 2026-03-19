package com.transporte.encomiendas.repository;

import com.transporte.encomiendas.entity.ParcelTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParcelTrackingRepository extends JpaRepository<ParcelTracking, UUID> {
    List<ParcelTracking> findByParcelIdOrderByTimestampDesc(UUID parcelId);
}
