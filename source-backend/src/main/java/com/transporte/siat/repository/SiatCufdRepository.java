package com.transporte.siat.repository;

import com.transporte.siat.entity.SiatCufd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiatCufdRepository extends JpaRepository<SiatCufd, UUID> {

    @Query("""
        SELECT c FROM SiatCufd c
        WHERE c.siatConfig.id = :configId
          AND c.codigoSucursal = :sucursal
          AND (c.codigoPuntoVenta = :puntoVenta OR (:puntoVenta IS NULL AND c.codigoPuntoVenta IS NULL))
          AND c.activo = true
          AND c.fechaVigencia > CURRENT_TIMESTAMP
        ORDER BY c.fechaVigencia DESC
        LIMIT 1
        """)
    Optional<SiatCufd> findVigente(UUID configId, Integer sucursal, Integer puntoVenta);
}
