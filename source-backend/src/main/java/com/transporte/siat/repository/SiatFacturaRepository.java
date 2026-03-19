package com.transporte.siat.repository;

import com.transporte.siat.entity.SiatFactura;
import com.transporte.siat.enums.SiatEstadoEmision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiatFacturaRepository extends JpaRepository<SiatFactura, UUID> {

    Optional<SiatFactura> findByCuf(String cuf);

    Optional<SiatFactura> findByInvoiceId(UUID invoiceId);

    Page<SiatFactura> findByEstadoEmision(SiatEstadoEmision estado, Pageable pageable);

    List<SiatFactura> findBySiatPaqueteId(UUID paqueteId);

    @Query("SELECT COALESCE(MAX(f.numeroFactura), 0) FROM SiatFactura f WHERE f.siatConfig.id = :configId AND f.codigoSucursal = :sucursal")
    Long findMaxNumeroFactura(UUID configId, Integer sucursal);
}
