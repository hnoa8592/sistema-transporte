package com.transporte.finanzas.repository;

import com.transporte.finanzas.entity.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemParameterRepository extends JpaRepository<SystemParameter, UUID> {
    Optional<SystemParameter> findByKeyAndActiveTrue(String key);
    List<SystemParameter> findAllByActiveTrue();
}
