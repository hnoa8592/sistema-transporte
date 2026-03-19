package com.transporte.siat.mapper;

import com.transporte.siat.dto.SiatConfigRequest;
import com.transporte.siat.dto.SiatConfigResponse;
import com.transporte.siat.entity.SiatConfig;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SiatConfigMapper {

    SiatConfigResponse toResponse(SiatConfig entity);

    @Mapping(target = "activo", constant = "true")
    SiatConfig toEntity(SiatConfigRequest request);

    @Mapping(target = "activo", ignore = true)
    void updateEntity(@MappingTarget SiatConfig entity, SiatConfigRequest request);
}
