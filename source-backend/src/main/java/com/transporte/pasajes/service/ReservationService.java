package com.transporte.pasajes.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.pasajes.dto.ReservationRequest;
import com.transporte.pasajes.dto.ReservationResponse;
import com.transporte.pasajes.entity.Reservation;
import com.transporte.pasajes.entity.Ticket;
import com.transporte.pasajes.enums.ReservationStatus;
import com.transporte.pasajes.enums.SeatStatus;
import com.transporte.pasajes.enums.TicketStatus;
import com.transporte.pasajes.mapper.ReservationMapper;
import com.transporte.pasajes.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TicketService ticketService;
    private final SeatMapService seatMapService;
    private final ReservationMapper reservationMapper;

    @Value("${app.reservation.expiry-minutes:30}")
    private int expiryMinutes;

    public PageResponse<ReservationResponse> findAll(Pageable pageable, ReservationStatus status) {
        if (status != null) {
            return PageResponse.of(reservationRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                    .map(reservationMapper::toResponse));
        }
        return PageResponse.of(reservationRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(reservationMapper::toResponse));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Reservation", description = "Creación de reserva de asiento")
    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        Ticket ticket = ticketService.findTicketById(request.ticketId());
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessException("No se puede reservar un pasaje cancelado");
        }

        reservationRepository.findByTicketIdAndStatus(request.ticketId(), ReservationStatus.PENDING)
                .ifPresent(r -> { throw new BusinessException("El pasaje ya tiene una reserva pendiente"); });

        seatMapService.updateSeatStatus(
                ticket.getScheduleId(), ticket.getTravelDate(),
                ticket.getSeatNumber(), ticket.getFloorNumber(), SeatStatus.RESERVED,
                ticket.getId()
        );

        Reservation reservation = Reservation.builder()
                .ticketId(request.ticketId())
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .status(ReservationStatus.PENDING)
                .notes(request.notes())
                .build();

        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Auditable(action = AuditAction.STATUS_CHANGE, entityType = "Reservation", description = "Confirmación de reserva de asiento")
    @Transactional
    public ReservationResponse confirm(UUID reservationId) {
        Reservation reservation = findReservationById(reservationId);
        if (reservation.isExpired()) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            throw new BusinessException("La reserva ha expirado");
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        Ticket ticket = ticketService.findTicketById(reservation.getTicketId());
        seatMapService.updateSeatStatus(
                ticket.getScheduleId(), ticket.getTravelDate(),
                ticket.getSeatNumber(), ticket.getFloorNumber(), SeatStatus.SOLD,
                ticket.getId()
        );
        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Auditable(action = AuditAction.CANCEL, entityType = "Reservation", description = "Cancelación de reserva de asiento")
    @Transactional
    public void cancel(UUID reservationId) {
        Reservation reservation = findReservationById(reservationId);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        Ticket ticket = ticketService.findTicketById(reservation.getTicketId());
        seatMapService.updateSeatStatus(
                ticket.getScheduleId(), ticket.getTravelDate(),
                ticket.getSeatNumber(), ticket.getFloorNumber(), SeatStatus.AVAILABLE,
                null
        );
    }

    private Reservation findReservationById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }
}
