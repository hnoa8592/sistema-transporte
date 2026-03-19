package com.transporte.siat.repository;

import com.transporte.siat.entity.SiatEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SiatEventoRepository extends JpaRepository<SiatEvento, UUID> {

    Page<SiatEvento> findBySiatConfigId(UUID configId, Pageable pageable);
}
