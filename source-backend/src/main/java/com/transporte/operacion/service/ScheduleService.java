package com.transporte.operacion.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.ScheduleRequest;
import com.transporte.operacion.dto.ScheduleResponse;
import com.transporte.operacion.entity.Bus;
import com.transporte.operacion.entity.Driver;
import com.transporte.operacion.entity.Route;
import com.transporte.operacion.entity.Schedule;
import com.transporte.operacion.mapper.ScheduleMapper;
import com.transporte.operacion.repository.BusRepository;
import com.transporte.operacion.repository.DriverRepository;
import com.transporte.operacion.repository.RouteRepository;
import com.transporte.operacion.repository.ScheduleRepository;
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
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final DriverRepository driverRepository;
    private final ScheduleMapper scheduleMapper;

    public PageResponse<ScheduleResponse> findAll(Pageable pageable) {
        return PageResponse.of(scheduleRepository.findAllByActiveTrue(pageable).map(scheduleMapper::toResponse));
    }

    public ScheduleResponse findById(UUID id) {
        return scheduleMapper.toResponse(findScheduleById(id));
    }

    @Cacheable(value = "schedules", key = "#routeId")
    public List<ScheduleResponse> findByRoute(UUID routeId) {
        return scheduleRepository.findByRouteId(routeId).stream()
                .map(scheduleMapper::toResponse).toList();
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Schedule", description = "Creación de nuevo horario de viaje")
    @Transactional
    @CacheEvict(value = "schedules", allEntries = true)
    public ScheduleResponse create(ScheduleRequest request) {
        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new ResourceNotFoundException("Route", request.routeId()));
        Bus bus = busRepository.findById(request.busId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", request.busId()));
        Driver driver = driverRepository.findById(request.driverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver", request.driverId()));

        Schedule schedule = scheduleMapper.toEntity(request);
        schedule.setRoute(route);
        schedule.setBus(bus);
        schedule.setDriver(driver);
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Schedule", description = "Actualización de horario de viaje")
    @Transactional
    @CacheEvict(value = "schedules", allEntries = true)
    public ScheduleResponse update(UUID id, ScheduleRequest request) {
        Schedule schedule = findScheduleById(id);
        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new ResourceNotFoundException("Route", request.routeId()));
        Bus bus = busRepository.findById(request.busId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", request.busId()));
        Driver driver = driverRepository.findById(request.driverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver", request.driverId()));
        scheduleMapper.updateFromRequest(request, schedule);
        schedule.setRoute(route);
        schedule.setBus(bus);
        schedule.setDriver(driver);
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Auditable(action = AuditAction.DELETE, entityType = "Schedule", description = "Desactivación de horario de viaje")
    @Transactional
    @CacheEvict(value = "schedules", allEntries = true)
    public void delete(UUID id) {
        Schedule schedule = findScheduleById(id);
        schedule.setActive(false);
        scheduleRepository.save(schedule);
    }

    private Schedule findScheduleById(UUID id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", id));
    }
}
