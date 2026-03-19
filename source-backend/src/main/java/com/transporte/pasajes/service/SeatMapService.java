package com.transporte.pasajes.service;

import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.pasajes.dto.SeatMapResponse;
import com.transporte.pasajes.entity.SeatMap;
import com.transporte.pasajes.enums.SeatStatus;
import com.transporte.pasajes.mapper.SeatMapMapper;
import com.transporte.pasajes.repository.SeatMapRepository;
import com.transporte.operacion.entity.Schedule;
import com.transporte.operacion.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatMapService {

    private final SeatMapRepository seatMapRepository;
    private final ScheduleRepository scheduleRepository;
    private final SeatMapMapper seatMapMapper;

    // @Transactional (read-write) overrides the class-level readOnly=true.
    // Required because generateSeatMap() is called via this.generateSeatMap() (same bean),
    // bypassing the Spring AOP proxy — so its own @Transactional is ignored.
    // Without this override the saveAll() inside generateSeatMap() runs under a
    // read-only transaction and seats are never persisted.
    @Transactional
    public List<SeatMapResponse> getSeatMap(UUID scheduleId, LocalDate travelDate) {
        List<SeatMap> seats = seatMapRepository.findByScheduleIdAndTravelDate(scheduleId, travelDate);
        if (seats.isEmpty()) {
            seats = generateSeatMap(scheduleId, travelDate);
        }
        return seats.stream().map(seatMapMapper::toResponse).toList();
    }

    @Transactional
    public List<SeatMap> generateSeatMap(UUID scheduleId, LocalDate travelDate) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", scheduleId));

        List<SeatMap> seats = new ArrayList<>();

        if (schedule.getBus().isHasTwoFloors()) {
            int floor1Seats = schedule.getBus().getSeatsFirstFloor() != null
                    ? schedule.getBus().getSeatsFirstFloor()
                    : schedule.getBus().getTotalSeats() / 2;
            int floor2Seats = schedule.getBus().getSeatsSecondFloor() != null
                    ? schedule.getBus().getSeatsSecondFloor()
                    : schedule.getBus().getTotalSeats() - floor1Seats;
            for (int i = 1; i <= floor1Seats; i++) {
                seats.add(buildSeat(scheduleId, travelDate, i, 1));
            }
            for (int i = 1; i <= floor2Seats; i++) {
                seats.add(buildSeat(scheduleId, travelDate, i, 2));
            }
        } else {
            for (int i = 1; i <= schedule.getBus().getTotalSeats(); i++) {
                seats.add(buildSeat(scheduleId, travelDate, i, 1));
            }
        }
        return seatMapRepository.saveAll(seats);
    }

    private SeatMap buildSeat(UUID scheduleId, LocalDate travelDate, int seatNumber, int floor) {
        return SeatMap.builder()
                .scheduleId(scheduleId)
                .travelDate(travelDate)
                .seatNumber(seatNumber)
                .floorNumber(floor)
                .status(SeatStatus.AVAILABLE)
                .build();
    }

    @Transactional
    public void updateSeatStatus(UUID scheduleId, LocalDate travelDate, int seatNumber, int floorNumber, SeatStatus status, UUID ticketId) {
        int updated = seatMapRepository.updateSeatStatus(scheduleId, travelDate, seatNumber, floorNumber, status, ticketId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Seat " + seatNumber + " (floor " + floorNumber + ") not found in seat map");
        }
    }

    public long countAvailableSeats(UUID scheduleId, LocalDate travelDate) {
        return seatMapRepository.countByScheduleIdAndTravelDateAndStatus(scheduleId, travelDate, SeatStatus.AVAILABLE);
    }
}
