package com.transporte.encomiendas.mapper;

import com.transporte.encomiendas.dto.ParcelRequest;
import com.transporte.encomiendas.dto.ParcelResponse;
import com.transporte.encomiendas.entity.Parcel;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ParcelMapper {
    Parcel toEntity(ParcelRequest request);
    ParcelResponse toResponse(Parcel parcel);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(ParcelRequest request, @MappingTarget Parcel parcel);
}
