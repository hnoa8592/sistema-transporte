package com.transporte.operacion.repository;

import com.transporte.operacion.entity.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
    Page<Location> findAllByActiveTrue(Pageable pageable);
    Page<Location> findAllByProvinceIdAndActiveTrue(UUID provinceId, Pageable pageable);
}
