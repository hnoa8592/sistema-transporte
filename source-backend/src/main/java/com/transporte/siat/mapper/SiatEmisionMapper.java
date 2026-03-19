package com.transporte.siat.mapper;

import com.transporte.siat.dto.SiatEmisionDetalleResponse;
import com.transporte.siat.dto.SiatEmisionResponse;
import com.transporte.siat.entity.SiatFactura;
import com.transporte.siat.entity.SiatFacturaDetalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SiatEmisionMapper {

    @Mapping(target = "detalles", source = "detalles")
    SiatEmisionResponse toResponse(SiatFactura entity);

    SiatEmisionDetalleResponse toDetalleResponse(SiatFacturaDetalle detalle);
}
