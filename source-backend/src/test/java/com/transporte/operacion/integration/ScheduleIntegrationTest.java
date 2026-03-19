package com.transporte.operacion.integration;

import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.ScheduleRequest;
import com.transporte.operacion.dto.ScheduleResponse;
import com.transporte.operacion.entity.*;
import com.transporte.operacion.repository.*;
import com.transporte.operacion.service.ScheduleService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Schedule lifecycle.
 *
 * Uses H2 in-memory database (application-test.yml) and OperacionTestConfig.
 * Tests run through the full stack: service → mapper → repository → H2.
 */
@SpringBootTest(
    classes = com.transporte.operacion.OperacionTestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Transactional
@DisplayName("Schedule Integration Tests")
class ScheduleIntegrationTest {

    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private RouteRepository routeRepository;
    @Autowired private BusRepository busRepository;
    @Autowired private DriverRepository driverRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private ProvinceRepository provinceRepository;
    @Autowired private DepartmentRepository departmentRepository;

    private UUID routeId;
    private UUID busId;
    private UUID driverId;

    // ---------------------------------------------------------------------------
    // Setup: create minimal dependency graph before each test
    // ---------------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        Department department = departmentRepository.save(
            Department.builder().name("Cochabamba").code("CB").active(true).build()
        );
        Province province = provinceRepository.save(
            Province.builder().name("Cercado").department(department).active(true).build()
        );
        Location origin = locationRepository.save(
            Location.builder().name("Terminal Cochabamba").province(province).active(true).build()
        );
        Location destination = locationRepository.save(
            Location.builder().name("Terminal La Paz").province(province).active(true).build()
        );

        Route route = routeRepository.save(Route.builder()
            .originLocation(origin)
            .destinationLocation(destination)
            .distanceKm(new BigDecimal("394.00"))
            .estimatedDurationMinutes(360)
            .basePrice(new BigDecimal("80.00"))
            .description("Cochabamba - La Paz")
            .active(true)
            .build()
        );
        routeId = route.getId();

        Bus bus = busRepository.save(Bus.builder()
            .plate("TEST-001")
            .model("Marcopolo")
            .brand("Mercedes")
            .year(2022)
            .totalSeats(42)
            .active(true)
            .build()
        );
        busId = bus.getId();

        Driver driver = driverRepository.save(Driver.builder()
            .dni("12345678")
            .firstName("Carlos")
            .lastName("Mamani")
            .licenseNumber("LIC-001")
            .licenseCategory("B")
            .licenseExpiryDate(LocalDate.of(2027, 12, 31))
            .active(true)
            .build()
        );
        driverId = driver.getId();
    }

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private ScheduleRequest buildRequest(String departure, String arrival, List<Integer> days) {
        return new ScheduleRequest(
            routeId, busId, driverId,
            LocalTime.parse(departure),
            arrival != null ? LocalTime.parse(arrival) : null,
            days, true, null
        );
    }

    // ---------------------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should create a schedule and persist it in the database")
    void shouldCreateSchedule() {
        ScheduleRequest request = buildRequest("08:00", "14:00", List.of(1, 2, 3, 4, 5));

        ScheduleResponse created = scheduleService.create(request);

        assertThat(created.id()).isNotNull();
        assertThat(created.routeId()).isEqualTo(routeId);
        assertThat(created.busId()).isEqualTo(busId);
        assertThat(created.driverId()).isEqualTo(driverId);
        assertThat(created.routeDescription()).isEqualTo("Cochabamba - La Paz");
        assertThat(created.busPlate()).isEqualTo("TEST-001");
        assertThat(created.driverName()).isEqualTo("Carlos Mamani");
        assertThat(created.departureTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(created.arrivalTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(created.daysOfWeek()).containsExactly(1, 2, 3, 4, 5);
        assertThat(created.active()).isTrue();

        assertThat(scheduleRepository.existsById(created.id())).isTrue();
    }

    @Test
    @DisplayName("Should create a schedule with no arrival time and no days")
    void shouldCreateScheduleWithMinimalFields() {
        ScheduleRequest request = buildRequest("22:30", null, List.of());

        ScheduleResponse created = scheduleService.create(request);

        assertThat(created.id()).isNotNull();
        assertThat(created.departureTime()).isEqualTo(LocalTime.of(22, 30));
        assertThat(created.arrivalTime()).isNull();
        assertThat(created.daysOfWeek()).isEmpty();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when route does not exist")
    void shouldThrowWhenRouteNotFound() {
        ScheduleRequest request = new ScheduleRequest(
            UUID.randomUUID(), busId, driverId,
            LocalTime.of(8, 0), null, List.of(1), true, null
        );

        assertThatThrownBy(() -> scheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Route");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when bus does not exist")
    void shouldThrowWhenBusNotFound() {
        ScheduleRequest request = new ScheduleRequest(
            routeId, UUID.randomUUID(), driverId,
            LocalTime.of(8, 0), null, List.of(1), true, null
        );

        assertThatThrownBy(() -> scheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Bus");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when driver does not exist")
    void shouldThrowWhenDriverNotFound() {
        ScheduleRequest request = new ScheduleRequest(
            routeId, busId, UUID.randomUUID(),
            LocalTime.of(8, 0), null, List.of(1), true, null
        );

        assertThatThrownBy(() -> scheduleService.create(request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Driver");
    }

    // ---------------------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should find schedule by ID after creation")
    void shouldFindScheduleById() {
        ScheduleResponse created = scheduleService.create(buildRequest("10:00", "16:00", List.of(6, 7)));

        ScheduleResponse found = scheduleService.findById(created.id());

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.departureTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(found.daysOfWeek()).containsExactly(6, 7);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when schedule ID does not exist")
    void shouldThrowWhenScheduleNotFound() {
        UUID nonExistent = UUID.randomUUID();

        assertThatThrownBy(() -> scheduleService.findById(nonExistent))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Schedule");
    }

    @Test
    @DisplayName("Should return paginated list of active schedules")
    void shouldReturnPaginatedSchedules() {
        scheduleService.create(buildRequest("07:00", "13:00", List.of(1, 3, 5)));
        scheduleService.create(buildRequest("14:00", "20:00", List.of(2, 4, 6)));
        scheduleService.create(buildRequest("21:00", null,    List.of(7)));

        PageResponse<ScheduleResponse> page = scheduleService.findAll(PageRequest.of(0, 10));

        assertThat(page.content()).hasSize(3);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.first()).isTrue();
    }

    @Test
    @DisplayName("Should only return active schedules in paginated list")
    void shouldExcludeInactiveSchedulesFromList() {
        ScheduleResponse active = scheduleService.create(buildRequest("08:00", "14:00", List.of(1)));
        ScheduleResponse toDelete = scheduleService.create(buildRequest("15:00", "21:00", List.of(2)));

        scheduleService.delete(toDelete.id());

        PageResponse<ScheduleResponse> page = scheduleService.findAll(PageRequest.of(0, 10));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).id()).isEqualTo(active.id());
    }

    @Test
    @DisplayName("Should find schedules by route ID")
    void shouldFindSchedulesByRoute() {
        scheduleService.create(buildRequest("08:00", "14:00", List.of(1, 2, 3)));
        scheduleService.create(buildRequest("20:00", null,    List.of(5, 6, 7)));

        List<ScheduleResponse> byRoute = scheduleService.findByRoute(routeId);

        assertThat(byRoute).hasSize(2);
        // Results ordered by departureTime
        assertThat(byRoute.get(0).departureTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(byRoute.get(1).departureTime()).isEqualTo(LocalTime.of(20, 0));
        assertThat(byRoute).allMatch(s -> s.routeId().equals(routeId));
    }

    @Test
    @DisplayName("Should return empty list when no schedules exist for a route")
    void shouldReturnEmptyListForRouteWithNoSchedules() {
        List<ScheduleResponse> result = scheduleService.findByRoute(routeId);
        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should update schedule departure time and days of week")
    void shouldUpdateSchedule() {
        ScheduleResponse created = scheduleService.create(buildRequest("08:00", "14:00", List.of(1, 2, 3)));

        ScheduleRequest updateRequest = new ScheduleRequest(
            routeId, busId, driverId,
            LocalTime.of(9, 30), LocalTime.of(15, 30),
            List.of(4, 5, 6, 7), true, "Updated notes"
        );
        ScheduleResponse updated = scheduleService.update(created.id(), updateRequest);

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.departureTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(updated.arrivalTime()).isEqualTo(LocalTime.of(15, 30));
        assertThat(updated.daysOfWeek()).containsExactly(4, 5, 6, 7);
        assertThat(updated.notes()).isEqualTo("Updated notes");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent schedule")
    void shouldThrowWhenUpdatingNonExistentSchedule() {
        ScheduleRequest request = buildRequest("08:00", "14:00", List.of(1));

        assertThatThrownBy(() -> scheduleService.update(UUID.randomUUID(), request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Schedule");
    }

    // ---------------------------------------------------------------------------
    // DELETE (soft delete)
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should soft-delete a schedule (mark active=false)")
    void shouldSoftDeleteSchedule() {
        ScheduleResponse created = scheduleService.create(buildRequest("08:00", "14:00", List.of(1)));

        scheduleService.delete(created.id());

        Schedule inDb = scheduleRepository.findById(created.id()).orElseThrow();
        assertThat(inDb.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent schedule")
    void shouldThrowWhenDeletingNonExistentSchedule() {
        assertThatThrownBy(() -> scheduleService.delete(UUID.randomUUID()))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Schedule");
    }
}
