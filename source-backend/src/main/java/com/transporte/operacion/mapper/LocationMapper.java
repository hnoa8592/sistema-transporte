package com.transporte.operacion.mapper;

import com.transporte.operacion.dto.LocationRequest;
import com.transporte.operacion.dto.LocationResponse;
import com.transporte.operacion.entity.Location;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "province", ignore = true)
    Location toEntity(LocationRequest request);

    @Mapping(target = "city", source = "province.name")
    @Mapping(target = "department", source = "province.department.name")
    LocationResponse toResponse(Location location);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "province", ignore = true)
    void updateFromRequest(LocationRequest request, @MappingTarget Location location);
}
