package com.transporte.auditoria.repository;

import com.transporte.auditoria.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByEntityType(String entityType, Pageable pageable);

    Page<AuditLog> findAllByUsername(String username, Pageable pageable);

    Page<AuditLog> findAllByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}
