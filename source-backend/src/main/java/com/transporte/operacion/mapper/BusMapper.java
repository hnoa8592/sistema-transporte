package com.transporte.operacion.mapper;

import com.transporte.operacion.dto.BusRequest;
import com.transporte.operacion.dto.BusResponse;
import com.transporte.operacion.entity.Bus;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BusMapper {
    @Mapping(target = "fleet", ignore = true)
    Bus toEntity(BusRequest request);

    @Mapping(target = "fleetId", source = "fleet.id")
    @Mapping(target = "fleetName", source = "fleet.name")
    BusResponse toResponse(Bus bus);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "fleet", ignore = true)
    void updateFromRequest(BusRequest request, @MappingTarget Bus bus);
}
