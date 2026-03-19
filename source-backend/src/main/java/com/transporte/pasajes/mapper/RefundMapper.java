package com.transporte.pasajes.mapper;

import com.transporte.pasajes.dto.RefundResponse;
import com.transporte.pasajes.entity.Refund;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefundMapper {
    RefundResponse toResponse(Refund refund);
}
