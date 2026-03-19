package com.transporte.siat.service;

import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.siat.dto.SiatConfigRequest;
import com.transporte.siat.dto.SiatConfigResponse;
import com.transporte.siat.entity.SiatConfig;
import com.transporte.siat.mapper.SiatConfigMapper;
import com.transporte.siat.repository.SiatConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiatConfigService {

    private final SiatConfigRepository configRepository;
    private final SiatConfigMapper configMapper;

    @Cacheable(value = "siat-config", key = "'all'")
    public List<SiatConfigResponse> findAll() {
        return configRepository.findAll().stream().map(configMapper::toResponse).toList();
    }

    @Cacheable(value = "siat-config", key = "#id")
    public SiatConfigResponse findById(UUID id) {
        return configMapper.toResponse(configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SiatConfig", id)));
    }

    @Transactional
    @CacheEvict(value = "siat-config", allEntries = true)
    public SiatConfigResponse create(SiatConfigRequest request) {
        SiatConfig config = configMapper.toEntity(request);
        return configMapper.toResponse(configRepository.save(config));
    }

    @Transactional
    @CacheEvict(value = "siat-config", allEntries = true)
    public SiatConfigResponse update(UUID id, SiatConfigRequest request) {
        SiatConfig config = configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SiatConfig", id));
        configMapper.updateEntity(config, request);
        return configMapper.toResponse(configRepository.save(config));
    }

    @Transactional
    @CacheEvict(value = "siat-config", allEntries = true)
    public void toggleActivo(UUID id) {
        SiatConfig config = configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SiatConfig", id));
        config.setActivo(!config.getActivo());
        configRepository.save(config);
    }

    /** Obtiene la configuración activa por tenant (para uso interno de otros servicios) */
    public SiatConfig getConfigActivaByTenant(String tenantId) {
        return configRepository.findFirstByTenantIdAndActivoTrue(tenantId)
                .orElseThrow(() -> new BusinessException("No existe configuración SIAT activa para este tenant"));
    }

    public SiatConfig getById(UUID id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SiatConfig", id));
    }
}
