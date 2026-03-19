package com.transporte.finanzas.repository;

import com.transporte.finanzas.entity.CashTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CashTransactionRepository extends JpaRepository<CashTransaction, UUID> {
    List<CashTransaction> findByCashRegisterIdOrderByCreatedAtDesc(UUID cashRegisterId);
    Page<CashTransaction> findByCashRegisterId(UUID cashRegisterId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CashTransaction t WHERE t.cashRegisterId = :cashRegisterId AND t.type = 'INGRESO'")
    BigDecimal sumIncomesByCashRegisterId(UUID cashRegisterId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CashTransaction t WHERE t.cashRegisterId = :cashRegisterId AND t.type = 'EGRESO'")
    BigDecimal sumExpensesByCashRegisterId(UUID cashRegisterId);
}
