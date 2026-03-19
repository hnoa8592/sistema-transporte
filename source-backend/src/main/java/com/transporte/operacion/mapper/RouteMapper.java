package com.transporte.operacion.mapper;

import com.transporte.operacion.dto.RouteRequest;
import com.transporte.operacion.dto.RouteResponse;
import com.transporte.operacion.entity.Route;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RouteMapper {
    @Mapping(target = "originLocation", ignore = true)
    @Mapping(target = "destinationLocation", ignore = true)
    Route toEntity(RouteRequest request);

    @Mapping(target = "originLocationId", source = "originLocation.id")
    @Mapping(target = "originLocationName", source = "originLocation.name")
    @Mapping(target = "destinationLocationId", source = "destinationLocation.id")
    @Mapping(target = "destinationLocationName", source = "destinationLocation.name")
    RouteResponse toResponse(Route route);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "originLocation", ignore = true)
    @Mapping(target = "destinationLocation", ignore = true)
    void updateFromRequest(RouteRequest request, @MappingTarget Route route);
}
