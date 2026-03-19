package com.transporte.siat.mapper;

import com.transporte.siat.dto.SiatCatalogoResponse;
import com.transporte.siat.entity.SiatCatalogo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SiatCatalogoMapper {
    SiatCatalogoResponse toResponse(SiatCatalogo entity);
}
