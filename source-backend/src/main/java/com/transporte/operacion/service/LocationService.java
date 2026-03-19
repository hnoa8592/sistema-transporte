package com.transporte.operacion.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.LocationRequest;
import com.transporte.operacion.dto.LocationResponse;
import com.transporte.operacion.entity.Location;
import com.transporte.operacion.entity.Province;
import com.transporte.operacion.mapper.LocationMapper;
import com.transporte.operacion.repository.LocationRepository;
import com.transporte.operacion.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationRepository locationRepository;
    private final ProvinceRepository provinceRepository;
    private final LocationMapper locationMapper;

    public PageResponse<LocationResponse> findAll(Pageable pageable) {
        return PageResponse.of(locationRepository.findAllByActiveTrue(pageable).map(locationMapper::toResponse));
    }

    public LocationResponse findById(UUID id) {
        return locationMapper.toResponse(findLocationById(id));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Location", description = "Creación de nueva terminal/localidad")
    @Transactional
    public LocationResponse create(LocationRequest request) {
        Province province = findProvinceById(request.provinceId());
        Location location = locationMapper.toEntity(request);
        location.setProvince(province);
        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Location", description = "Actualización de datos de terminal/localidad")
    @Transactional
    public LocationResponse update(UUID id, LocationRequest request) {
        Location location = findLocationById(id);
        if (request.provinceId() != null) {
            location.setProvince(findProvinceById(request.provinceId()));
        }
        locationMapper.updateFromRequest(request, location);
        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Auditable(action = AuditAction.DELETE, entityType = "Location", description = "Desactivación de terminal/localidad")
    @Transactional
    public void delete(UUID id) {
        Location location = findLocationById(id);
        location.setActive(false);
        locationRepository.save(location);
    }

    private Location findLocationById(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", id));
    }

    private Province findProvinceById(UUID id) {
        return provinceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Province", id));
    }
}
