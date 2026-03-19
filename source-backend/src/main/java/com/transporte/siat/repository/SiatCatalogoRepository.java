package com.transporte.siat.repository;

import com.transporte.siat.entity.SiatCatalogo;
import com.transporte.siat.enums.SiatTipoCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiatCatalogoRepository extends JpaRepository<SiatCatalogo, UUID> {

    List<SiatCatalogo> findByTipoCatalogoAndVigenteTrue(SiatTipoCatalogo tipo);

    Optional<SiatCatalogo> findByTipoCatalogoAndCodigoAndVigenteTrue(SiatTipoCatalogo tipo, String codigo);

    void deleteByTipoCatalogoAndTenantId(SiatTipoCatalogo tipo, String tenantId);
}
