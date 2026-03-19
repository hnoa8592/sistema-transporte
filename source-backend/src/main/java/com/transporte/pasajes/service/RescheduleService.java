package com.transporte.pasajes.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.service.CustomerService;
import com.transporte.pasajes.dto.RescheduleRequest;
import com.transporte.pasajes.dto.RescheduleResponse;
import com.transporte.pasajes.dto.TicketRequest;
import com.transporte.pasajes.dto.TicketResponse;
import com.transporte.pasajes.entity.Reschedule;
import com.transporte.pasajes.entity.Ticket;
import com.transporte.pasajes.enums.SeatStatus;
import com.transporte.pasajes.enums.TicketStatus;
import com.transporte.pasajes.mapper.RescheduleMapper;
import com.transporte.pasajes.repository.RescheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RescheduleService {

    private final RescheduleRepository rescheduleRepository;
    private final TicketService ticketService;
    private final SeatMapService seatMapService;
    private final RescheduleMapper rescheduleMapper;
    private final CustomerService customerService;

    public PageResponse<RescheduleResponse> findAll(Pageable pageable) {
        return PageResponse.of(rescheduleRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(rescheduleMapper::toResponse));
    }

    @Auditable(action = AuditAction.RESCHEDULE, entityType = "Reschedule", description = "Reprogramación de pasaje a otro horario")
    @Transactional
    public RescheduleResponse reschedule(RescheduleRequest request) {
        Ticket originalTicket = ticketService.findTicketById(request.originalTicketId());
        if (originalTicket.getStatus() != TicketStatus.CONFIRMED) {
            throw new BusinessException("Solo se pueden reprogramar pasajes confirmados");
        }

        // Cancel original ticket seat
        seatMapService.updateSeatStatus(
                originalTicket.getScheduleId(), originalTicket.getTravelDate(),
                originalTicket.getSeatNumber(), originalTicket.getFloorNumber(), SeatStatus.AVAILABLE,
                null
        );
        originalTicket.setStatus(TicketStatus.RESCHEDULED);

        // Create new ticket
        TicketRequest newTicketRequest = new TicketRequest(
                request.newScheduleId(),
                originalTicket.getCustomerId(),
                request.newSeatNumber() > 0 ? request.newSeatNumber() : originalTicket.getSeatNumber(),
                request.newFloorNumber() > 0 ? request.newFloorNumber() : originalTicket.getFloorNumber(),
                request.newTravelDate(),
                originalTicket.getPrice(),
                originalTicket.getSaleType(),
                request.employeeId(),
                originalTicket.getPassengerName(),
                originalTicket.getPassengerDocument(),
                customerService.findById(originalTicket.getCustomerId()).documentType()
        );
        TicketResponse newTicket = ticketService.create(newTicketRequest);

        Reschedule reschedule = Reschedule.builder()
                .originalTicketId(request.originalTicketId())
                .newTicketId(newTicket.id())
                .newScheduleId(request.newScheduleId())
                .reason(request.reason())
                .fee(request.fee() != null ? request.fee() : BigDecimal.ZERO)
                .employeeId(request.employeeId())
                .build();

        return rescheduleMapper.toResponse(rescheduleRepository.save(reschedule));
    }
}
