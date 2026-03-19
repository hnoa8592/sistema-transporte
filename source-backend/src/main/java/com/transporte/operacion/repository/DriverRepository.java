package com.transporte.operacion.repository;

import com.transporte.operacion.entity.Driver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {
    Page<Driver> findAllByActiveTrue(Pageable pageable);
    Optional<Driver> findByDni(String dni);
    boolean existsByDni(String dni);
}
