package com.transporte.tenants.dto;

import com.transporte.tenants.enums.TenantPlan;
import jakarta.validation.constraints.*;

public record TenantRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{2,49}$",
                message = "schemaName must start with a lowercase letter and contain only lowercase letters, digits, or underscores (3-50 chars)")
        String schemaName,
        @NotBlank @Email String contactEmail,
        String contactPhone,
        String address,
        TenantPlan plan
) {}
