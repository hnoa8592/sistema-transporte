package com.transporte.pasajes.repository;

import com.transporte.pasajes.entity.Ticket;
import com.transporte.pasajes.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    Optional<Ticket> findByTicketCode(String ticketCode);
    Page<Ticket> findAllByStatusNot(TicketStatus status, Pageable pageable);
    List<Ticket> findByScheduleIdAndTravelDateAndStatusNot(UUID scheduleId, LocalDate travelDate, TicketStatus status);
    List<Ticket> findByCustomerIdAndStatusNot(UUID customerId, TicketStatus status, Pageable pageable);
    boolean existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
            UUID scheduleId, LocalDate travelDate, int seatNumber, int floorNumber, TicketStatus status);
}
