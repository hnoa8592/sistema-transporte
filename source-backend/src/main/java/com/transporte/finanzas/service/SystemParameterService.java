package com.transporte.finanzas.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.finanzas.dto.SystemParameterRequest;
import com.transporte.finanzas.dto.SystemParameterResponse;
import com.transporte.finanzas.entity.SystemParameter;
import com.transporte.finanzas.mapper.SystemParameterMapper;
import com.transporte.finanzas.repository.SystemParameterRepository;
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
public class SystemParameterService {

    private final SystemParameterRepository parameterRepository;
    private final SystemParameterMapper parameterMapper;

    @Cacheable(value = "parameters")
    public List<SystemParameterResponse> findAll() {
        return parameterRepository.findAllByActiveTrue().stream()
                .map(parameterMapper::toResponse).toList();
    }

    public SystemParameterResponse findById(UUID id) {
        return parameterMapper.toResponse(parameterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemParameter", id)));
    }

    @Cacheable(value = "parameters", key = "#key")
    public String getValue(String key, String defaultValue) {
        return parameterRepository.findByKeyAndActiveTrue(key)
                .map(SystemParameter::getValue)
                .orElse(defaultValue);
    }

    @Auditable(action = AuditAction.CREATE, entityType = "SystemParameter", description = "Creación de parámetro del sistema")
    @Transactional
    @CacheEvict(value = "parameters", allEntries = true)
    public SystemParameterResponse create(SystemParameterRequest request) {
        if (parameterRepository.findByKeyAndActiveTrue(request.key()).isPresent()) {
            throw new BusinessException("Parameter with key '" + request.key() + "' already exists");
        }
        SystemParameter parameter = parameterMapper.toEntity(request);
        return parameterMapper.toResponse(parameterRepository.save(parameter));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "SystemParameter", description = "Actualización de parámetro del sistema")
    @Transactional
    @CacheEvict(value = "parameters", allEntries = true)
    public SystemParameterResponse update(UUID id, SystemParameterRequest request) {
        SystemParameter parameter = parameterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemParameter", id));
        parameterMapper.updateFromRequest(request, parameter);
        return parameterMapper.toResponse(parameterRepository.save(parameter));
    }
}
