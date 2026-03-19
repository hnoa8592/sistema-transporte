package com.transporte.tenants.dto;

import com.transporte.tenants.enums.TenantStatus;
import jakarta.validation.constraints.NotNull;

public record TenantStatusRequest(
        @NotNull TenantStatus status
) {}
