package com.transporte.pasajes.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.core.util.CodeGenerator;
import com.transporte.operacion.entity.Customer;
import com.transporte.operacion.repository.CustomerRepository;
import com.transporte.pasajes.dto.*;
import com.transporte.pasajes.entity.Ticket;
import com.transporte.pasajes.enums.SaleType;
import com.transporte.pasajes.enums.SeatStatus;
import com.transporte.pasajes.enums.TicketStatus;
import com.transporte.pasajes.mapper.TicketMapper;
import com.transporte.pasajes.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final SeatMapService seatMapService;
    private final TicketMapper ticketMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomerRepository customerRepository;

    public PageResponse<TicketResponse> findAll(Pageable pageable) {
        return PageResponse.of(ticketRepository.findAllByStatusNot(TicketStatus.CANCELLED, pageable)
                .map(ticketMapper::toResponse));
    }

    public TicketResponse findById(UUID id) {
        return ticketMapper.toResponse(findTicketById(id));
    }

    public TicketResponse findByCode(String code) {
        return ticketMapper.toResponse(
                ticketRepository.findByTicketCode(code)
                        .orElseThrow(() -> new ResourceNotFoundException("Pasaje con código " + code + " no encontrado"))
        );
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Ticket", description = "Venta de pasaje")
    @Transactional
    public TicketResponse create(TicketRequest request) {
        // Check seat availability
        boolean seatTaken = ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                request.scheduleId(), request.travelDate(),
                request.seatNumber(), request.floorNumber(), TicketStatus.CANCELLED
        );
        if (seatTaken) {
            throw new BusinessException("El asiento " + request.seatNumber() + " (piso " + request.floorNumber() + ") ya está ocupado");
        }

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setTicketCode(CodeGenerator.generateTicketCode("TKT"));
        ticket.setSaleType(request.saleType() != null ? request.saleType() : SaleType.VENTANILLA);
        ticket.setStatus(TicketStatus.CONFIRMED);

        ticket = ticketRepository.save(ticket);

        // Update seat map
        seatMapService.updateSeatStatus(
                request.scheduleId(), request.travelDate(),
                request.seatNumber(), request.floorNumber(), SeatStatus.SOLD,
                ticket.getId()
        );

        log.info("Pasaje {} creado para el horario {} asiento {}", ticket.getTicketCode(), request.scheduleId(), request.seatNumber());
        return ticketMapper.toResponse(ticket);
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Ticket", description = "Venta masiva de pasajes")
    @Transactional
    public List<TicketResponse> createBulk(BulkTicketRequest request) {
        return request.tickets().stream()
                .map(ticketReq -> create(resolveOrRegisterCustomer(ticketReq)))
                .toList();
    }

    /**
     * Si el pasaje no tiene customerId pero sí passengerDocument,
     * busca el cliente por documento o lo registra automáticamente.
     */
    private TicketRequest resolveOrRegisterCustomer(TicketRequest req) {
        if (req.customerId() != null
                || req.passengerDocument() == null
                || req.passengerDocument().isBlank()) {
            return req;
        }

        UUID customerId = customerRepository.findByDocumentNumber(req.passengerDocument())
                .map(Customer::getId)
                .orElseGet(() -> {
                    Customer newCustomer = buildCustomerFromTicket(req);
                    log.info("Registrando nuevo cliente con documento {} durante venta masiva", req.passengerDocument());
                    return customerRepository.save(newCustomer).getId();
                });

        return new TicketRequest(
                req.scheduleId(), customerId, req.seatNumber(), req.floorNumber(),
                req.travelDate(), req.price(), req.saleType(), req.employeeId(),
                req.passengerName(), req.passengerDocument(), req.passengerDocumentType()
        );
    }

    private Customer buildCustomerFromTicket(TicketRequest req) {
        String firstName = req.passengerName() != null ? req.passengerName() : "";
        String lastName = "";
        if (req.passengerName() != null) {
            int spaceIdx = req.passengerName().indexOf(' ');
            if (spaceIdx > 0) {
                firstName = req.passengerName().substring(0, spaceIdx);
                lastName = req.passengerName().substring(spaceIdx + 1);
            }
        }
        return Customer.builder()
                .documentNumber(req.passengerDocument())
                .documentType(req.passengerDocumentType())
                .firstName(firstName)
                .lastName(lastName)
                .active(true)
                .build();
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Ticket", description = "Actualización de datos del pasajero en el pasaje")
    @Transactional
    public TicketResponse updateCustomerInfo(UUID id, UpdateTicketCustomerRequest request) {
        Ticket ticket = findTicketById(id);
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessException("No se puede actualizar un pasaje cancelado");
        }
        if (request.customerId() != null) ticket.setCustomerId(request.customerId());
        if (request.passengerName() != null) ticket.setPassengerName(request.passengerName());
        if (request.passengerDocument() != null) ticket.setPassengerDocument(request.passengerDocument());
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Auditable(action = AuditAction.UPDATE, entityType = "Ticket", description = "Cambio de asiento en el pasaje")
    @Transactional
    public TicketResponse changeSeat(UUID id, ChangeSeatRequest request) {
        Ticket ticket = findTicketById(id);
        if (ticket.getStatus() == TicketStatus.CANCELLED || ticket.getStatus() == TicketStatus.RESCHEDULED) {
            throw new BusinessException("No se puede cambiar el asiento de un pasaje con estado " + ticket.getStatus());
        }

        boolean seatTaken = ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                ticket.getScheduleId(), ticket.getTravelDate(),
                request.newSeatNumber(), request.newFloorNumber(), TicketStatus.CANCELLED
        );
        if (seatTaken) {
            throw new BusinessException("El nuevo asiento " + request.newSeatNumber() + " ya está ocupado");
        }

        // Release old seat
        seatMapService.updateSeatStatus(
                ticket.getScheduleId(), ticket.getTravelDate(),
                ticket.getSeatNumber(), ticket.getFloorNumber(), SeatStatus.AVAILABLE,
                ticket.getId()
        );

        // Block new seat
        seatMapService.updateSeatStatus(
                ticket.getScheduleId(), ticket.getTravelDate(),
                request.newSeatNumber(), request.newFloorNumber(), SeatStatus.SOLD,
                ticket.getId()
        );

        ticket.setSeatNumber(request.newSeatNumber());
        ticket.setFloorNumber(request.newFloorNumber());
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Auditable(action = AuditAction.CANCEL, entityType = "Ticket", description = "Cancelación de pasaje")
    @Transactional
    public void cancel(UUID id) {
        Ticket ticket = findTicketById(id);
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessException("El pasaje ya está cancelado");
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);

        seatMapService.updateSeatStatus(
                ticket.getScheduleId(), ticket.getTravelDate(),
                ticket.getSeatNumber(), ticket.getFloorNumber(), SeatStatus.AVAILABLE,
                ticket.getId()
        );
    }

    Ticket findTicketById(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }
}
