package com.transporte.operacion.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.RouteRequest;
import com.transporte.operacion.dto.RouteResponse;
import com.transporte.operacion.entity.Location;
import com.transporte.operacion.entity.Route;
import com.transporte.operacion.mapper.RouteMapper;
import com.transporte.operacion.repository.LocationRepository;
import com.transporte.operacion.repository.RouteRepository;
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
public class RouteService {

    private final RouteRepository routeRepository;
    private final LocationRepository locationRepository;
    private final RouteMapper routeMapper;

    public PageResponse<RouteResponse> findAll(Pageable pageable) {
        return PageResponse.of(routeRepository.findAllByActiveTrue(pageable).map(routeMapper::toResponse));
    }

    @Cacheable(value = "routes", key = "#id")
    public RouteResponse findById(UUID id) {
        return routeMapper.toResponse(findRouteById(id));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Route", description = "Creación de nueva ruta de transporte")
    @Transactional
    @CacheEvict(value = "routes", allEntries = true)
    public RouteResponse create(RouteRequest request) {
        Location origin = locationRepository.findById(request.originLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", request.originLocationId()));
        Location destination = locationRepository.findById(request.destinationLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location", request.destinationLocationId()));
        Route route = routeMapper.toEntity(request);
        route.setOriginLocation(origin);
        route.setDestinationLocation(destination);
        return routeMapper.toResponse(routeRepository.save(route));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Route", description = "Actualización de datos de la ruta")
    @Transactional
    @CacheEvict(value = "routes", allEntries = true)
    public RouteResponse update(UUID id, RouteRequest request) {
        Route route = findRouteById(id);
        if (request.originLocationId() != null) {
            Location origin = locationRepository.findById(request.originLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location", request.originLocationId()));
            route.setOriginLocation(origin);
        }
        if (request.destinationLocationId() != null) {
            Location destination = locationRepository.findById(request.destinationLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location", request.destinationLocationId()));
            route.setDestinationLocation(destination);
        }
        routeMapper.updateFromRequest(request, route);
        return routeMapper.toResponse(routeRepository.save(route));
    }

    @Auditable(action = AuditAction.DELETE, entityType = "Route", description = "Desactivación de ruta de transporte")
    @Transactional
    @CacheEvict(value = "routes", allEntries = true)
    public void delete(UUID id) {
        Route route = findRouteById(id);
        route.setActive(false);
        routeRepository.save(route);
    }

    private Route findRouteById(UUID id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route", id));
    }
}
