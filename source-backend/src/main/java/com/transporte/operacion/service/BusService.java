package com.transporte.operacion.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.BusRequest;
import com.transporte.operacion.dto.BusResponse;
import com.transporte.operacion.entity.Bus;
import com.transporte.operacion.entity.Fleet;
import com.transporte.operacion.mapper.BusMapper;
import com.transporte.operacion.repository.BusRepository;
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
public class BusService {

    private final BusRepository busRepository;
    private final FleetRepository fleetRepository;
    private final BusMapper busMapper;

    public PageResponse<BusResponse> findAll(Pageable pageable) {
        return PageResponse.of(busRepository.findAllByActiveTrue(pageable).map(busMapper::toResponse));
    }

    @Cacheable(value = "buses", key = "#id")
    public BusResponse findById(UUID id) {
        return busMapper.toResponse(findBusById(id));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Bus", description = "Registro de nuevo bus en la flota")
    @Transactional
    @CacheEvict(value = "buses", allEntries = true)
    public BusResponse create(BusRequest request) {
        if (busRepository.existsByPlate(request.plate())) {
            throw new BusinessException("Bus with plate '" + request.plate() + "' already exists");
        }
        Bus bus = busMapper.toEntity(request);
        if (request.fleetId() != null) {
            Fleet fleet = fleetRepository.findById(request.fleetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fleet", request.fleetId()));
            bus.setFleet(fleet);
        }
        return busMapper.toResponse(busRepository.save(bus));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Bus", description = "Actualización de datos del bus")
    @Transactional
    @CacheEvict(value = "buses", allEntries = true)
    public BusResponse update(UUID id, BusRequest request) {
        Bus bus = findBusById(id);
        busMapper.updateFromRequest(request, bus);
        if (request.fleetId() != null) {
            Fleet fleet = fleetRepository.findById(request.fleetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fleet", request.fleetId()));
            bus.setFleet(fleet);
        }
        return busMapper.toResponse(busRepository.save(bus));
    }

    @Auditable(action = AuditAction.DELETE, entityType = "Bus", description = "Baja de bus de la flota")
    @Transactional
    @CacheEvict(value = "buses", allEntries = true)
    public void delete(UUID id) {
        Bus bus = findBusById(id);
        bus.setActive(false);
        busRepository.save(bus);
    }

    private Bus findBusById(UUID id) {
        return busRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", id));
    }
}
