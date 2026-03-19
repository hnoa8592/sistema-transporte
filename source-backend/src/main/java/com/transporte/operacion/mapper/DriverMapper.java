package com.transporte.operacion.mapper;

import com.transporte.operacion.dto.DriverRequest;
import com.transporte.operacion.dto.DriverResponse;
import com.transporte.operacion.entity.Driver;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DriverMapper {
    Driver toEntity(DriverRequest request);
    DriverResponse toResponse(Driver driver);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(DriverRequest request, @MappingTarget Driver driver);
}
