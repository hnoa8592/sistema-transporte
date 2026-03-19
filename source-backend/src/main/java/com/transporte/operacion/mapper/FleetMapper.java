package com.transporte.operacion.mapper;

import com.transporte.operacion.dto.FleetRequest;
import com.transporte.operacion.dto.FleetResponse;
import com.transporte.operacion.entity.Fleet;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface FleetMapper {

    Fleet toEntity(FleetRequest request);

    FleetResponse toResponse(Fleet fleet);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(FleetRequest request, @MappingTarget Fleet fleet);
}
