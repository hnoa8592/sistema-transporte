package com.transporte.tenants.mapper;

import com.transporte.tenants.dto.TenantRequest;
import com.transporte.tenants.dto.TenantResponse;
import com.transporte.tenants.entity.Tenant;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    Tenant toEntity(TenantRequest request);

    TenantResponse toResponse(Tenant tenant);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(TenantRequest request, @MappingTarget Tenant tenant);
}
