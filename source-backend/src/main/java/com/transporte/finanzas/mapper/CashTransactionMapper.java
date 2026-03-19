package com.transporte.finanzas.mapper;

import com.transporte.finanzas.dto.CashTransactionRequest;
import com.transporte.finanzas.dto.CashTransactionResponse;
import com.transporte.finanzas.entity.CashTransaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CashTransactionMapper {
    CashTransaction toEntity(CashTransactionRequest request);
    CashTransactionResponse toResponse(CashTransaction transaction);
}
