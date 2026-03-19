package com.transporte.pasajes.repository;

import com.transporte.pasajes.entity.Refund;
import com.transporte.pasajes.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {
    Optional<Refund> findByTicketId(UUID ticketId);
    Page<Refund> findAllByStatus(RefundStatus status, Pageable pageable);
    boolean existsByTicketId(UUID ticketId);
}
