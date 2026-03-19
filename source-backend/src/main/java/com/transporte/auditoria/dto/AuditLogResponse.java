package com.transporte.auditoria.dto;

import com.transporte.auditoria.enums.AuditAction;
import com.transporte.auditoria.enums.AuditStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String tenantId,
        String userId,
        String username,
        AuditAction action,
        String entityType,
        String entityId,
        String httpMethod,
        String endpoint,
        String ipAddress,
        String description,
        AuditStatus status,
        String errorMessage,
        LocalDateTime createdAt
) {}
