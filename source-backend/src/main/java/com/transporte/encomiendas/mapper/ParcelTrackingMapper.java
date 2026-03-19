package com.transporte.encomiendas.mapper;

import com.transporte.encomiendas.dto.ParcelTrackingResponse;
import com.transporte.encomiendas.entity.ParcelTracking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParcelTrackingMapper {
    ParcelTrackingResponse toResponse(ParcelTracking tracking);
}
