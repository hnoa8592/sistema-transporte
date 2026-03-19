package com.transporte.pasajes.repository;

import com.transporte.pasajes.entity.Reservation;
import com.transporte.pasajes.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByTicketIdAndStatus(UUID ticketId, ReservationStatus status);

    Page<Reservation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Reservation> findByStatusOrderByCreatedAtDesc(ReservationStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Reservation r SET r.status = 'EXPIRED' WHERE r.expiresAt < CURRENT_TIMESTAMP AND r.status = 'PENDING'")
    int expireReservations();
}
