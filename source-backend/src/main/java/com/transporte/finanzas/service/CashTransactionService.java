package com.transporte.finanzas.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.finanzas.dto.CashTransactionRequest;
import com.transporte.finanzas.dto.CashTransactionResponse;
import com.transporte.finanzas.entity.CashTransaction;
import com.transporte.finanzas.enums.CashRegisterStatus;
import com.transporte.finanzas.mapper.CashTransactionMapper;
import com.transporte.finanzas.repository.CashRegisterRepository;
import com.transporte.finanzas.repository.CashTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashTransactionService {

    private final CashTransactionRepository cashTransactionRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final CashTransactionMapper cashTransactionMapper;

    public PageResponse<CashTransactionResponse> findByCashRegister(UUID cashRegisterId, Pageable pageable) {
        return PageResponse.of(cashTransactionRepository.findByCashRegisterId(cashRegisterId, pageable)
                .map(cashTransactionMapper::toResponse));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "CashTransaction", description = "Registro de movimiento en caja")
    @Transactional
    public CashTransactionResponse create(CashTransactionRequest request) {
        cashRegisterRepository.findById(request.cashRegisterId())
                .filter(cr -> cr.getStatus() == CashRegisterStatus.OPEN)
                .orElseThrow(() -> new BusinessException("Cash register not found or is not open"));
        CashTransaction transaction = cashTransactionMapper.toEntity(request);
        return cashTransactionMapper.toResponse(cashTransactionRepository.save(transaction));
    }
}
