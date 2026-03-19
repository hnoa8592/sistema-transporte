package com.transporte.operacion.mapper;

import com.transporte.operacion.dto.CustomerRequest;
import com.transporte.operacion.dto.CustomerResponse;
import com.transporte.operacion.entity.Customer;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CustomerMapper {
    Customer toEntity(CustomerRequest request);
    CustomerResponse toResponse(Customer customer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(CustomerRequest request, @MappingTarget Customer customer);
}
