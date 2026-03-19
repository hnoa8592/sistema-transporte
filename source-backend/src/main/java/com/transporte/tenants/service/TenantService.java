package com.transporte.tenants.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.tenants.dto.TenantRequest;
import com.transporte.tenants.dto.TenantResponse;
import com.transporte.tenants.dto.TenantStatusRequest;
import com.transporte.tenants.entity.Tenant;
import com.transporte.tenants.enums.TenantPlan;
import com.transporte.tenants.mapper.TenantMapper;
import com.transporte.tenants.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;
    private final TenantProvisioningService provisioningService;

    public PageResponse<TenantResponse> findAll(Pageable pageable) {
        return PageResponse.of(tenantRepository.findAll(pageable).map(tenantMapper::toResponse));
    }

    public TenantResponse findById(UUID id) {
        return tenantMapper.toResponse(findTenantById(id));
    }

    public TenantResponse findBySchemaName(String schemaName) {
        return tenantMapper.toResponse(
                tenantRepository.findBySchemaName(schemaName)
                        .orElseThrow(() -> new ResourceNotFoundException("Tenant with schemaName " + schemaName + " not found"))
        );
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Tenant", description = "Registro de nueva empresa en el sistema")
    @Transactional
    public TenantResponse create(TenantRequest request) {
        if (tenantRepository.existsByName(request.name())) {
            throw new BusinessException("Ya existe una empresa con el nombre '" + request.name() + "'");
        }
        if (tenantRepository.existsBySchemaName(request.schemaName())) {
            throw new BusinessException("Ya existe una empresa con el esquema '" + request.schemaName() + "'");
        }

        Tenant tenant = tenantMapper.toEntity(request);
        if (tenant.getPlan() == null) {
            tenant.setPlan(TenantPlan.BASIC);
        }
        tenant = tenantRepository.save(tenant);

        provisioningService.provisionTenant(tenant.getSchemaName());

        log.info("Empresa '{}' creada con esquema '{}'", tenant.getName(), tenant.getSchemaName());
        return tenantMapper.toResponse(tenant);
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Tenant", description = "Actualización de datos de la empresa")
    @Transactional
    public TenantResponse update(UUID id, TenantRequest request) {
        Tenant tenant = findTenantById(id);

        if (!tenant.getName().equals(request.name()) && tenantRepository.existsByName(request.name())) {
            throw new BusinessException("Ya existe una empresa con el nombre '" + request.name() + "'");
        }

        tenantMapper.updateFromRequest(request, tenant);
        tenant = tenantRepository.save(tenant);

        log.info("Empresa '{}' actualizada", tenant.getId());
        return tenantMapper.toResponse(tenant);
    }

    @Auditable(action = AuditAction.STATUS_CHANGE, entityType = "Tenant", description = "Cambio de estado de la empresa")
    @Transactional
    public TenantResponse updateStatus(UUID id, TenantStatusRequest request) {
        Tenant tenant = findTenantById(id);
        tenant.setStatus(request.status());
        tenant = tenantRepository.save(tenant);

        log.info("Empresa '{}' cambió su estado a {}", tenant.getId(), request.status());
        return tenantMapper.toResponse(tenant);
    }

    private Tenant findTenantById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
    }
}
