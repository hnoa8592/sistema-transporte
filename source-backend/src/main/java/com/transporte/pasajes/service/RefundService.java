package com.transporte.pasajes.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.pasajes.dto.RefundRequest;
import com.transporte.pasajes.dto.RefundResponse;
import com.transporte.pasajes.entity.Refund;
import com.transporte.pasajes.entity.Ticket;
import com.transporte.pasajes.enums.RefundStatus;
import com.transporte.pasajes.enums.TicketStatus;
import com.transporte.pasajes.mapper.RefundMapper;
import com.transporte.pasajes.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundService {

    private final RefundRepository refundRepository;
    private final TicketService ticketService;
    private final RefundMapper refundMapper;

    public PageResponse<RefundResponse> findAll(Pageable pageable) {
        return PageResponse.of(refundRepository.findAllByStatus(RefundStatus.PENDING, pageable)
                .map(refundMapper::toResponse));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Refund", description = "Solicitud de devolución de pasaje")
    @Transactional
    public RefundResponse requestRefund(RefundRequest request) {
        Ticket ticket = ticketService.findTicketById(request.ticketId());
        if (ticket.getStatus() != TicketStatus.CONFIRMED) {
            throw new BusinessException("Solo se pueden devolver pasajes confirmados");
        }
        if (refundRepository.existsByTicketId(request.ticketId())) {
            throw new BusinessException("Ya existe una solicitud de devolución para este pasaje");
        }

        BigDecimal retentionPercent = request.retentionPercent() != null ? request.retentionPercent() : BigDecimal.ZERO;
        BigDecimal originalAmount = ticket.getPrice();
        BigDecimal retainedAmount = originalAmount.multiply(retentionPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal refundedAmount = originalAmount.subtract(retainedAmount);

        Refund refund = Refund.builder()
                .ticketId(request.ticketId())
                .reason(request.reason())
                .retentionPercent(retentionPercent)
                .originalAmount(originalAmount)
                .retainedAmount(retainedAmount)
                .refundedAmount(refundedAmount)
                .status(RefundStatus.PENDING)
                .employeeId(request.employeeId())
                .build();

        return refundMapper.toResponse(refundRepository.save(refund));
    }

    @Auditable(action = AuditAction.APPROVE, entityType = "Refund", description = "Aprobación de devolución de pasaje")
    @Transactional
    public RefundResponse approve(UUID refundId) {
        Refund refund = findRefundById(refundId);
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BusinessException("La devolución no está en estado PENDIENTE");
        }
        refund.setStatus(RefundStatus.APPROVED);
        ticketService.cancel(refund.getTicketId());
        return refundMapper.toResponse(refundRepository.save(refund));
    }

    @Auditable(action = AuditAction.REJECT, entityType = "Refund", description = "Rechazo de solicitud de devolución")
    @Transactional
    public RefundResponse reject(UUID refundId, String reason) {
        Refund refund = findRefundById(refundId);
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BusinessException("La devolución no está en estado PENDIENTE");
        }
        refund.setStatus(RefundStatus.REJECTED);
        refund.setReason(reason);
        return refundMapper.toResponse(refundRepository.save(refund));
    }

    private Refund findRefundById(UUID id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund", id));
    }
}
