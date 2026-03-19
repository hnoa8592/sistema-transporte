package com.transporte.pasajes.service;

import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.operacion.entity.Bus;
import com.transporte.operacion.entity.Schedule;
import com.transporte.operacion.repository.ScheduleRepository;
import com.transporte.pasajes.dto.SeatMapResponse;
import com.transporte.pasajes.entity.SeatMap;
import com.transporte.pasajes.enums.SeatStatus;
import com.transporte.pasajes.mapper.SeatMapMapper;
import com.transporte.pasajes.repository.SeatMapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatMapService Unit Tests")
class SeatMapServiceTest {

    @Mock private SeatMapRepository seatMapRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private SeatMapMapper seatMapMapper;

    @InjectMocks
    private SeatMapService seatMapService;

    private UUID scheduleId;
    private LocalDate travelDate;

    @BeforeEach
    void setUp() {
        scheduleId = UUID.randomUUID();
        travelDate = LocalDate.now().plusDays(1);
    }

    // Helper: build a single-floor bus
    private Bus buildSingleFloorBus(int totalSeats) {
        Bus bus = new Bus();
        bus.setHasTwoFloors(false);
        bus.setTotalSeats(totalSeats);
        bus.setSeatsFirstFloor(null);
        bus.setSeatsSecondFloor(null);
        bus.setPlate("ABC-123");
        return bus;
    }

    // Helper: build a two-floor bus
    private Bus buildTwoFloorBus(int totalSeats, Integer floor1, Integer floor2) {
        Bus bus = new Bus();
        bus.setHasTwoFloors(true);
        bus.setTotalSeats(totalSeats);
        bus.setSeatsFirstFloor(floor1);
        bus.setSeatsSecondFloor(floor2);
        bus.setPlate("XYZ-456");
        return bus;
    }

    private Schedule buildScheduleWithBus(Bus bus) {
        Schedule schedule = new Schedule();
        try {
            var idField = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(schedule, UUID.randomUUID());
        } catch (Exception e) {
            // ignore
        }
        schedule.setBus(bus);
        return schedule;
    }

    private SeatMap buildSeatMap(UUID sid, LocalDate date, int seatNum, int floor, SeatStatus status) {
        return SeatMap.builder()
                .scheduleId(sid)
                .travelDate(date)
                .seatNumber(seatNum)
                .floorNumber(floor)
                .status(status)
                .build();
    }

    private SeatMapResponse buildSeatMapResponse(int seatNum, int floor, SeatStatus status) {
        return new SeatMapResponse(UUID.randomUUID(), scheduleId, travelDate, seatNum, floor, status, null);
    }

    @Nested
    @DisplayName("getSeatMap() tests")
    class GetSeatMapTests {

        @Test
        @DisplayName("Should return existing seat map when already generated")
        void shouldReturnExistingSeatMapWhenAlreadyGenerated() {
            List<SeatMap> existingSeats = List.of(
                    buildSeatMap(scheduleId, travelDate, 1, 1, SeatStatus.AVAILABLE),
                    buildSeatMap(scheduleId, travelDate, 2, 1, SeatStatus.SOLD),
                    buildSeatMap(scheduleId, travelDate, 3, 1, SeatStatus.AVAILABLE)
            );
            SeatMapResponse resp1 = buildSeatMapResponse(1, 1, SeatStatus.AVAILABLE);
            SeatMapResponse resp2 = buildSeatMapResponse(2, 1, SeatStatus.SOLD);
            SeatMapResponse resp3 = buildSeatMapResponse(3, 1, SeatStatus.AVAILABLE);

            given(seatMapRepository.findByScheduleIdAndTravelDate(scheduleId, travelDate))
                    .willReturn(existingSeats);
            given(seatMapMapper.toResponse(existingSeats.get(0))).willReturn(resp1);
            given(seatMapMapper.toResponse(existingSeats.get(1))).willReturn(resp2);
            given(seatMapMapper.toResponse(existingSeats.get(2))).willReturn(resp3);

            List<SeatMapResponse> result = seatMapService.getSeatMap(scheduleId, travelDate);

            assertThat(result).hasSize(3);
            // Should NOT call scheduleRepository since seats already exist
            verify(scheduleRepository, never()).findById(any());
            verify(seatMapRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should generate seat map when none exists")
        void shouldGenerateSeatMapWhenNoneExists() {
            Bus bus = buildSingleFloorBus(5);
            Schedule schedule = buildScheduleWithBus(bus);

            List<SeatMap> generatedSeats = List.of(
                    buildSeatMap(scheduleId, travelDate, 1, 1, SeatStatus.AVAILABLE),
                    buildSeatMap(scheduleId, travelDate, 2, 1, SeatStatus.AVAILABLE),
                    buildSeatMap(scheduleId, travelDate, 3, 1, SeatStatus.AVAILABLE),
                    buildSeatMap(scheduleId, travelDate, 4, 1, SeatStatus.AVAILABLE),
                    buildSeatMap(scheduleId, travelDate, 5, 1, SeatStatus.AVAILABLE)
            );

            given(seatMapRepository.findByScheduleIdAndTravelDate(scheduleId, travelDate))
                    .willReturn(Collections.emptyList());
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(seatMapRepository.saveAll(anyList())).willReturn(generatedSeats);
            given(seatMapMapper.toResponse(any())).willReturn(buildSeatMapResponse(1, 1, SeatStatus.AVAILABLE));

            List<SeatMapResponse> result = seatMapService.getSeatMap(scheduleId, travelDate);

            assertThat(result).hasSize(5);
            verify(scheduleRepository).findById(scheduleId);
            verify(seatMapRepository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("generateSeatMap() tests")
    class GenerateSeatMapTests {

        @Test
        @DisplayName("Should generate correct number of seats for single-floor bus")
        void shouldGenerateSeatsForSingleFloorBus() {
            int totalSeats = 40;
            Bus bus = buildSingleFloorBus(totalSeats);
            Schedule schedule = buildScheduleWithBus(bus);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(seatMapRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            List<SeatMap> result = seatMapService.generateSeatMap(scheduleId, travelDate);

            assertThat(result).hasSize(totalSeats);
            // All seats should be on floor 1
            assertThat(result).allMatch(s -> s.getFloorNumber() == 1);
            // All seats should be AVAILABLE
            assertThat(result).allMatch(s -> s.getStatus() == SeatStatus.AVAILABLE);
            // Seat numbers should be 1..40
            assertThat(result).extracting(SeatMap::getSeatNumber)
                    .containsExactlyInAnyOrder(
                            1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                            11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                            21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
                            31, 32, 33, 34, 35, 36, 37, 38, 39, 40
                    );
        }

        @Test
        @DisplayName("Should generate seats for two-floor bus with explicit floor counts")
        void shouldGenerateSeatsForTwoFloorBusWithExplicitCounts() {
            Bus bus = buildTwoFloorBus(60, 30, 30);
            Schedule schedule = buildScheduleWithBus(bus);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(seatMapRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            List<SeatMap> result = seatMapService.generateSeatMap(scheduleId, travelDate);

            assertThat(result).hasSize(60);

            long floor1Count = result.stream().filter(s -> s.getFloorNumber() == 1).count();
            long floor2Count = result.stream().filter(s -> s.getFloorNumber() == 2).count();

            assertThat(floor1Count).isEqualTo(30);
            assertThat(floor2Count).isEqualTo(30);

            // All seats should be AVAILABLE
            assertThat(result).allMatch(s -> s.getStatus() == SeatStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Should generate seats for two-floor bus deriving floor counts from totalSeats")
        void shouldGenerateSeatsForTwoFloorBusDerivingCounts() {
            // seatsFirstFloor and seatsSecondFloor are null; derived from totalSeats
            Bus bus = buildTwoFloorBus(40, null, null);
            Schedule schedule = buildScheduleWithBus(bus);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(seatMapRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            List<SeatMap> result = seatMapService.generateSeatMap(scheduleId, travelDate);

            assertThat(result).hasSize(40);
            long floor1Count = result.stream().filter(s -> s.getFloorNumber() == 1).count();
            long floor2Count = result.stream().filter(s -> s.getFloorNumber() == 2).count();
            // totalSeats / 2 = 20 for each floor
            assertThat(floor1Count).isEqualTo(20);
            assertThat(floor2Count).isEqualTo(20);
        }

        @Test
        @DisplayName("Should generate seats for two-floor bus with asymmetric floor distribution")
        void shouldGenerateSeatsForTwoFloorBusAsymmetric() {
            Bus bus = buildTwoFloorBus(40, 16, 24);
            Schedule schedule = buildScheduleWithBus(bus);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(seatMapRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            List<SeatMap> result = seatMapService.generateSeatMap(scheduleId, travelDate);

            assertThat(result).hasSize(40);
            long floor1Count = result.stream().filter(s -> s.getFloorNumber() == 1).count();
            long floor2Count = result.stream().filter(s -> s.getFloorNumber() == 2).count();
            assertThat(floor1Count).isEqualTo(16);
            assertThat(floor2Count).isEqualTo(24);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when schedule does not exist")
        void shouldThrowExceptionWhenScheduleNotFound() {
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> seatMapService.generateSeatMap(scheduleId, travelDate))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(seatMapRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should set scheduleId and travelDate correctly on each generated seat")
        void shouldSetScheduleIdAndTravelDateOnSeats() {
            Bus bus = buildSingleFloorBus(3);
            Schedule schedule = buildScheduleWithBus(bus);

            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(seatMapRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

            List<SeatMap> result = seatMapService.generateSeatMap(scheduleId, travelDate);

            assertThat(result).allMatch(s -> scheduleId.equals(s.getScheduleId()));
            assertThat(result).allMatch(s -> travelDate.equals(s.getTravelDate()));
        }
    }

    @Nested
    @DisplayName("updateSeatStatus() tests")
    class UpdateSeatStatusTests {

        @Test
        @DisplayName("Should update seat status successfully")
        void shouldUpdateSeatStatusSuccessfully() {
            given(seatMapRepository.updateSeatStatus(eq(scheduleId), eq(travelDate), eq(5), eq(1), eq(SeatStatus.SOLD), any(UUID.class)))
                    .willReturn(1);

            // Should not throw
            assertThatCode(() -> seatMapService.updateSeatStatus(scheduleId, travelDate, 5, 1, SeatStatus.SOLD, UUID.randomUUID()))
                    .doesNotThrowAnyException();

            verify(seatMapRepository).updateSeatStatus(eq(scheduleId), eq(travelDate), eq(5), eq(1), eq(SeatStatus.SOLD), any(UUID.class));
        }

        @Test
        @DisplayName("Should update seat status to AVAILABLE successfully")
        void shouldUpdateSeatStatusToAvailable() {
            given(seatMapRepository.updateSeatStatus(eq(scheduleId), eq(travelDate), eq(3), eq(2), eq(SeatStatus.AVAILABLE), any(UUID.class)))
                    .willReturn(1);

            assertThatCode(() -> seatMapService.updateSeatStatus(scheduleId, travelDate, 3, 2, SeatStatus.AVAILABLE, UUID.randomUUID()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when seat does not exist in map")
        void shouldThrowExceptionWhenSeatNotFoundInMap() {
            given(seatMapRepository.updateSeatStatus(eq(scheduleId), eq(travelDate), eq(99), eq(1), eq(SeatStatus.SOLD), any(UUID.class)))
                    .willReturn(0);

            assertThatThrownBy(() -> seatMapService.updateSeatStatus(scheduleId, travelDate, 99, 1, SeatStatus.SOLD, UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99")
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when floor does not exist in map")
        void shouldThrowExceptionWhenFloorNotFoundInMap() {
            given(seatMapRepository.updateSeatStatus(eq(scheduleId), eq(travelDate), eq(5), eq(3), eq(SeatStatus.RESERVED), any(UUID.class)))
                    .willReturn(0);

            assertThatThrownBy(() -> seatMapService.updateSeatStatus(scheduleId, travelDate, 5, 3, SeatStatus.RESERVED, UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("countAvailableSeats() tests")
    class CountAvailableSeatsTests {

        @Test
        @DisplayName("Should return correct count of available seats")
        void shouldReturnCorrectAvailableCount() {
            given(seatMapRepository.countByScheduleIdAndTravelDateAndStatus(
                    scheduleId, travelDate, SeatStatus.AVAILABLE)).willReturn(35L);

            long result = seatMapService.countAvailableSeats(scheduleId, travelDate);

            assertThat(result).isEqualTo(35L);
            verify(seatMapRepository).countByScheduleIdAndTravelDateAndStatus(
                    scheduleId, travelDate, SeatStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Should return zero when no seats are available")
        void shouldReturnZeroWhenNoSeatsAvailable() {
            given(seatMapRepository.countByScheduleIdAndTravelDateAndStatus(
                    scheduleId, travelDate, SeatStatus.AVAILABLE)).willReturn(0L);

            long result = seatMapService.countAvailableSeats(scheduleId, travelDate);

            assertThat(result).isZero();
        }

        @Test
        @DisplayName("Should return full seat count when all seats are available")
        void shouldReturnFullCountWhenAllSeatsAvailable() {
            given(seatMapRepository.countByScheduleIdAndTravelDateAndStatus(
                    scheduleId, travelDate, SeatStatus.AVAILABLE)).willReturn(40L);

            long result = seatMapService.countAvailableSeats(scheduleId, travelDate);

            assertThat(result).isEqualTo(40L);
        }
    }
}
