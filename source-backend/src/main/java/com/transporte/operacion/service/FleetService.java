package com.transporte.operacion.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.FleetRequest;
import com.transporte.operacion.dto.FleetResponse;
import com.transporte.operacion.entity.Fleet;
import com.transporte.operacion.mapper.FleetMapper;
import com.transporte.operacion.repository.FleetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FleetService {

    private final FleetRepository fleetRepository;
    private final FleetMapper fleetMapper;

    public PageResponse<FleetResponse> findAll(Pageable pageable) {
        return PageResponse.of(fleetRepository.findAll(pageable).map(fleetMapper::toResponse));
    }

    @Cacheable(value = "fleets", key = "#id")
    public FleetResponse findById(UUID id) {
        return fleetMapper.toResponse(findFleetById(id));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Fleet", description = "Creación de nueva flota")
    @Transactional
    @CacheEvict(value = "fleets", allEntries = true)
    public FleetResponse create(FleetRequest request) {
        if (fleetRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException("Fleet with name '" + request.name() + "' already exists");
        }
        return fleetMapper.toResponse(fleetRepository.save(fleetMapper.toEntity(request)));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Fleet", description = "Actualización de datos de la flota")
    @Transactional
    @CacheEvict(value = "fleets", allEntries = true)
    public FleetResponse update(UUID id, FleetRequest request) {
        Fleet fleet = findFleetById(id);
        if (!fleet.getName().equalsIgnoreCase(request.name())
                && fleetRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException("Fleet with name '" + request.name() + "' already exists");
        }
        fleetMapper.updateFromRequest(request, fleet);
        return fleetMapper.toResponse(fleetRepository.save(fleet));
    }

    @Auditable(action = AuditAction.DELETE, entityType = "Fleet", description = "Desactivación de flota")
    @Transactional
    @CacheEvict(value = "fleets", allEntries = true)
    public void delete(UUID id) {
        Fleet fleet = findFleetById(id);
        fleet.setActive(false);
        fleetRepository.save(fleet);
    }

    private Fleet findFleetById(UUID id) {
        return fleetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet", id));
    }
}
