package com.transporte.finanzas.mapper;

import com.transporte.finanzas.dto.CashRegisterRequest;
import com.transporte.finanzas.dto.CashRegisterResponse;
import com.transporte.finanzas.entity.CashRegister;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CashRegisterMapper {
    CashRegister toEntity(CashRegisterRequest request);
    CashRegisterResponse toResponse(CashRegister cashRegister);
}
