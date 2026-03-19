package com.transporte.pasajes.repository;

import com.transporte.pasajes.entity.SeatMap;
import com.transporte.pasajes.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatMapRepository extends JpaRepository<SeatMap, UUID> {
    List<SeatMap> findByScheduleIdAndTravelDate(UUID scheduleId, LocalDate travelDate);
    Optional<SeatMap> findByScheduleIdAndTravelDateAndSeatNumberAndFloorNumber(
            UUID scheduleId, LocalDate travelDate, int seatNumber, int floorNumber);
    long countByScheduleIdAndTravelDateAndStatus(UUID scheduleId, LocalDate travelDate, SeatStatus status);

    @Modifying
    @Query("UPDATE SeatMap s SET s.status = :status, s.ticketId = :ticketId WHERE s.scheduleId = :scheduleId AND s.travelDate = :travelDate AND s.seatNumber = :seatNumber AND s.floorNumber = :floorNumber")
    int updateSeatStatus(UUID scheduleId, LocalDate travelDate, int seatNumber, int floorNumber, SeatStatus status, UUID ticketId);
}
