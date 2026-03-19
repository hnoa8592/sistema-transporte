package com.transporte.siat.repository;

import com.transporte.siat.entity.SiatConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiatConfigRepository extends JpaRepository<SiatConfig, UUID> {

    Optional<SiatConfig> findByNitAndCodigoSucursalAndCodigoPuntoVentaAndActivoTrue(
            String nit, Integer codigoSucursal, Integer codigoPuntoVenta);

    Optional<SiatConfig> findFirstByTenantIdAndActivoTrue(String tenantId);
}
