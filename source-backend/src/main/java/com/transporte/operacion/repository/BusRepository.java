package com.transporte.operacion.repository;

import com.transporte.operacion.entity.Bus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusRepository extends JpaRepository<Bus, UUID> {
    Page<Bus> findAllByActiveTrue(Pageable pageable);
    Optional<Bus> findByPlate(String plate);
    boolean existsByPlate(String plate);
}
