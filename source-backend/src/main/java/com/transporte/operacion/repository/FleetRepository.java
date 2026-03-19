package com.transporte.operacion.repository;

import com.transporte.operacion.entity.Fleet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FleetRepository extends JpaRepository<Fleet, UUID> {
    Page<Fleet> findAllByActiveTrue(Pageable pageable);
    boolean existsByNameIgnoreCase(String name);
}
