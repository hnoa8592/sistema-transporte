package com.transporte.pasajes.repository;

import com.transporte.pasajes.entity.Reschedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RescheduleRepository extends JpaRepository<Reschedule, UUID> {
    Page<Reschedule> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Reschedule> findByOriginalTicketId(UUID ticketId, Pageable pageable);
}
