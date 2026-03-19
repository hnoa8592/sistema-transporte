package com.transporte.finanzas.repository;

import com.transporte.finanzas.entity.Invoice;
import com.transporte.finanzas.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Page<Invoice> findAllByStatus(InvoiceStatus status, Pageable pageable);
    Page<Invoice> findAll(Pageable pageable);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(i.invoiceNumber, LENGTH(i.invoiceNumber) - 7, 8) AS long)), 0) FROM Invoice i")
    Long findMaxSequence();
}
