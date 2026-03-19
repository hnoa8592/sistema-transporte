package com.transporte.finanzas.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.finanzas.dto.*;
import com.transporte.finanzas.entity.CashRegister;
import com.transporte.finanzas.enums.CashRegisterStatus;
import com.transporte.finanzas.mapper.CashRegisterMapper;
import com.transporte.finanzas.repository.CashRegisterRepository;
import com.transporte.finanzas.repository.CashTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final CashRegisterMapper cashRegisterMapper;

    public PageResponse<CashRegisterResponse> findAll(Pageable pageable) {
        return PageResponse.of(cashRegisterRepository.findAll(pageable).map(cashRegisterMapper::toResponse));
    }

    public CashRegisterResponse findById(UUID id) {
        return cashRegisterMapper.toResponse(findCashRegisterById(id));
    }

    public Optional<CashRegisterResponse> findOpenByEmployee(UUID employeeId) {
        return cashRegisterRepository.findByEmployeeIdAndStatus(employeeId, CashRegisterStatus.OPEN)
                .map(cashRegisterMapper::toResponse);
    }

    @Auditable(action = AuditAction.OPEN, entityType = "CashRegister", description = "Apertura de caja")
    @Transactional
    public CashRegisterResponse open(CashRegisterRequest request) {
        if (cashRegisterRepository.existsByEmployeeIdAndStatus(request.employeeId(), CashRegisterStatus.OPEN)) {
            throw new BusinessException("El empleado ya tiene una caja abierta");
        }
        CashRegister cashRegister = cashRegisterMapper.toEntity(request);
        cashRegister.setOpenedAt(LocalDateTime.now());
        cashRegister.setStatus(CashRegisterStatus.OPEN);
        return cashRegisterMapper.toResponse(cashRegisterRepository.save(cashRegister));
    }

    @Auditable(action = AuditAction.CLOSE, entityType = "CashRegister", description = "Cierre de caja")
    @Transactional
    public CashRegisterResponse close(UUID id, CloseCashRegisterRequest request) {
        CashRegister cashRegister = findCashRegisterById(id);
        if (cashRegister.getStatus() == CashRegisterStatus.CLOSED) {
            throw new BusinessException("La caja ya está cerrada");
        }
        cashRegister.setStatus(CashRegisterStatus.CLOSED);
        cashRegister.setClosedAt(LocalDateTime.now());
        cashRegister.setFinalAmount(request.finalAmount());
        if (request.notes() != null) cashRegister.setNotes(request.notes());
        return cashRegisterMapper.toResponse(cashRegisterRepository.save(cashRegister));
    }

    public CashRegisterSummaryResponse getSummary(UUID id) {
        CashRegister cashRegister = findCashRegisterById(id);
        BigDecimal totalIncomes = cashTransactionRepository.sumIncomesByCashRegisterId(id);
        BigDecimal totalExpenses = cashTransactionRepository.sumExpensesByCashRegisterId(id);
        BigDecimal expectedFinal = cashRegister.getInitialAmount().add(totalIncomes).subtract(totalExpenses);
        return new CashRegisterSummaryResponse(id, cashRegister.getInitialAmount(), totalIncomes, totalExpenses, expectedFinal);
    }

    private CashRegister findCashRegisterById(UUID id) {
        return cashRegisterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CashRegister", id));
    }
}
