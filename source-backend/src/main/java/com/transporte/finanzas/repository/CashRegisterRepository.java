package com.transporte.finanzas.repository;

import com.transporte.finanzas.entity.CashRegister;
import com.transporte.finanzas.enums.CashRegisterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, UUID> {
    Page<CashRegister> findAll(Pageable pageable);
    Optional<CashRegister> findByEmployeeIdAndStatus(UUID employeeId, CashRegisterStatus status);
    boolean existsByEmployeeIdAndStatus(UUID employeeId, CashRegisterStatus status);
}
