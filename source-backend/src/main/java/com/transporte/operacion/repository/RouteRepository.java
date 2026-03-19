package com.transporte.operacion.repository;

import com.transporte.operacion.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {
    Page<Route> findAllByActiveTrue(Pageable pageable);

    @Query("SELECT r FROM Route r WHERE r.originLocation.id = :originId AND r.destinationLocation.id = :destinationId AND r.active = true")
    Optional<Route> findByOriginAndDestination(UUID originId, UUID destinationId);
}
