package com.transporte.operacion.repository;

import com.transporte.operacion.entity.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    Page<Schedule> findAllByActiveTrue(Pageable pageable);

    @Query("SELECT s FROM Schedule s WHERE s.route.id = :routeId AND s.active = true ORDER BY s.departureTime")
    List<Schedule> findByRouteId(UUID routeId);
}
