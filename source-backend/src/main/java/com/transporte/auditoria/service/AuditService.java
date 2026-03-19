package com.transporte.auditoria.service;

import com.transporte.auditoria.dto.AuditLogResponse;
import com.transporte.auditoria.entity.AuditLog;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.auditoria.enums.AuditStatus;
import com.transporte.auditoria.repository.AuditLogRepository;
import com.transporte.core.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String tenantId, String userId, String username,
                    AuditAction action, String entityType, String entityId,
                    String httpMethod, String endpoint, String ipAddress,
                    AuditStatus status, String errorMessage, String description) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .httpMethod(httpMethod)
                    .endpoint(endpoint)
                    .ipAddress(ipAddress)
                    .status(status)
                    .errorMessage(errorMessage)
                    .description(description)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Error al guardar el registro de auditoría: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> findAll(Pageable pageable) {
        return PageResponse.of(auditLogRepository.findAll(pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> findByEntityType(String entityType, Pageable pageable) {
        return PageResponse.of(auditLogRepository.findAllByEntityType(entityType, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> findByUsername(String username, Pageable pageable) {
        return PageResponse.of(auditLogRepository.findAllByUsername(username, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> findByDateRange(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return PageResponse.of(auditLogRepository.findAllByCreatedAtBetween(from, to, pageable).map(this::toResponse));
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getTenantId(),
                log.getUserId(),
                log.getUsername(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getHttpMethod(),
                log.getEndpoint(),
                log.getIpAddress(),
                log.getDescription(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getCreatedAt()
        );
    }
}
