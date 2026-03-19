package com.transporte.siat.mapper;

import com.transporte.siat.dto.SiatCuisResponse;
import com.transporte.siat.entity.SiatCuis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SiatCuisMapper {

    @Mapping(target = "vigente", expression = "java(entity.isVigente())")
    SiatCuisResponse toResponse(SiatCuis entity);
}
