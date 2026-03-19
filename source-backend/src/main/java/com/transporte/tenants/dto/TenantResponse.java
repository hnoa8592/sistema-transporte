package com.transporte.tenants.dto;

import com.transporte.tenants.enums.TenantPlan;
import com.transporte.tenants.enums.TenantStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String schemaName,
        String contactEmail,
        String contactPhone,
        String address,
        boolean active,
        TenantPlan plan,
        TenantStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
