package com.transporte.operacion.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.DriverRequest;
import com.transporte.operacion.dto.DriverResponse;
import com.transporte.operacion.entity.Driver;
import com.transporte.operacion.mapper.DriverMapper;
import com.transporte.operacion.repository.DriverRepository;
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
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    public PageResponse<DriverResponse> findAll(Pageable pageable) {
        return PageResponse.of(driverRepository.findAllByActiveTrue(pageable).map(driverMapper::toResponse));
    }

    @Cacheable(value = "drivers", key = "#id")
    public DriverResponse findById(UUID id) {
        return driverMapper.toResponse(findDriverById(id));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Driver", description = "Registro de nuevo chofer")
    @Transactional
    @CacheEvict(value = "drivers", allEntries = true)
    public DriverResponse create(DriverRequest request) {
        if (driverRepository.existsByDni(request.dni())) {
            throw new BusinessException("Driver with DNI '" + request.dni() + "' already exists");
        }
        Driver driver = driverMapper.toEntity(request);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Driver", description = "Actualización de datos del chofer")
    @Transactional
    @CacheEvict(value = "drivers", allEntries = true)
    public DriverResponse update(UUID id, DriverRequest request) {
        Driver driver = findDriverById(id);
        if (!driver.getDni().equals(request.dni()) && driverRepository.existsByDni(request.dni())) {
            throw new BusinessException("Driver with DNI '" + request.dni() + "' already exists");
        }
        driverMapper.updateFromRequest(request, driver);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Auditable(action = AuditAction.DELETE, entityType = "Driver", description = "Baja de chofer del sistema")
    @Transactional
    @CacheEvict(value = "drivers", allEntries = true)
    public void delete(UUID id) {
        Driver driver = findDriverById(id);
        driver.setActive(false);
        driverRepository.save(driver);
    }

    private Driver findDriverById(UUID id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));
    }
}
