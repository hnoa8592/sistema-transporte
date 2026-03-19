package com.transporte.usuarios.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.usuarios.dto.ResourceRequest;
import com.transporte.usuarios.dto.ResourceResponse;
import com.transporte.usuarios.entity.Resource;
import com.transporte.usuarios.mapper.ResourceMapper;
import com.transporte.usuarios.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;

    public PageResponse<ResourceResponse> findAll(Pageable pageable) {
        return PageResponse.of(resourceRepository.findAllByActiveTrue(pageable).map(resourceMapper::toResponse));
    }

    @Cacheable(value = "resources", key = "'all'")
    public List<ResourceResponse> findAllActive() {
        return resourceRepository.findAllByActiveTrue().stream().map(resourceMapper::toResponse).toList();
    }

    @Cacheable(value = "resources", key = "#id")
    public ResourceResponse findById(UUID id) {
        return resourceMapper.toResponse(findResourceById(id));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Resource", description = "Creación de nuevo recurso de seguridad")
    @Transactional
    @CacheEvict(value = "resources", allEntries = true)
    public ResourceResponse create(ResourceRequest request) {
        if (resourceRepository.existsByHttpMethodAndEndpoint(request.httpMethod(), request.endpoint())) {
            throw new BusinessException("Resource with method " + request.httpMethod() + " and endpoint " + request.endpoint() + " already exists");
        }
        Resource resource = resourceMapper.toEntity(request);
        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Resource", description = "Actualización de recurso de seguridad")
    @Transactional
    @CacheEvict(value = "resources", allEntries = true)
    public ResourceResponse update(UUID id, ResourceRequest request) {
        Resource resource = findResourceById(id);
        resourceMapper.updateFromRequest(request, resource);
        return resourceMapper.toResponse(resourceRepository.save(resource));
    }

    @Auditable(action = AuditAction.DELETE, entityType = "Resource", description = "Desactivación de recurso de seguridad")
    @Transactional
    @CacheEvict(value = "resources", allEntries = true)
    public void delete(UUID id) {
        Resource resource = findResourceById(id);
        resource.setActive(false);
        resourceRepository.save(resource);
    }

    private Resource findResourceById(UUID id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
    }
}
