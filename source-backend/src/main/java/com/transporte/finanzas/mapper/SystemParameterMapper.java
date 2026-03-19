package com.transporte.finanzas.mapper;

import com.transporte.finanzas.dto.SystemParameterRequest;
import com.transporte.finanzas.dto.SystemParameterResponse;
import com.transporte.finanzas.entity.SystemParameter;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SystemParameterMapper {
    SystemParameter toEntity(SystemParameterRequest request);
    SystemParameterResponse toResponse(SystemParameter parameter);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(SystemParameterRequest request, @MappingTarget SystemParameter parameter);
}
