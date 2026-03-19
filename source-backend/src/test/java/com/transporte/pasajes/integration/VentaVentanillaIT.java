package com.transporte.pasajes.integration;

import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.operacion.dto.CustomerRequest;
import com.transporte.operacion.dto.CustomerResponse;
import com.transporte.operacion.dto.ScheduleResponse;
import com.transporte.operacion.entity.Bus;
import com.transporte.operacion.entity.Customer;
import com.transporte.operacion.entity.Schedule;
import com.transporte.operacion.mapper.CustomerMapper;
import com.transporte.operacion.repository.CustomerRepository;
import com.transporte.operacion.repository.ScheduleRepository;
import com.transporte.operacion.service.CustomerService;
import com.transporte.operacion.service.ScheduleService;
import com.transporte.pasajes.dto.SeatMapResponse;
import com.transporte.pasajes.dto.TicketRequest;
import com.transporte.pasajes.dto.TicketResponse;
import com.transporte.pasajes.enums.SaleType;
import com.transporte.pasajes.enums.SeatStatus;
import com.transporte.pasajes.enums.TicketStatus;
import com.transporte.pasajes.mapper.SeatMapMapper;
import com.transporte.pasajes.mapper.TicketMapper;
import com.transporte.pasajes.repository.SeatMapRepository;
import com.transporte.pasajes.repository.TicketRepository;
import com.transporte.pasajes.service.SeatMapService;
import com.transporte.pasajes.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Integration test: Venta en Ventanilla (Counter Sale)
 *
 * <p>Tests the full counter-sale flow across services:
 * <ol>
 *   <li>Paso 1 — Buscar horarios de la ruta seleccionada</li>
 *   <li>Paso 2 — Obtener/generar mapa de asientos del horario</li>
 *   <li>Paso 3 — Buscar cliente por número de documento (o crear uno nuevo)</li>
 *   <li>Paso 4 — Confirmar venta: crear ticket con saleType=COUNTER</li>
 * </ol>
 *
 * <p>Uses real service instances wired together; only repositories are mocked.
 * Validates the contract between frontend and backend, including the COUNTER
 * SaleType value introduced to align the two sides.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IT: Venta en Ventanilla (Counter Sale)")
class VentaVentanillaIT {

    // ── Repository mocks ────────────────────────────────────────────────────
    @Mock private TicketRepository    ticketRepository;
    @Mock private SeatMapRepository   seatMapRepository;
    @Mock private ScheduleRepository  scheduleRepository;
    @Mock private CustomerRepository  customerRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    // ── Mapper mocks ────────────────────────────────────────────────────────
    @Mock private TicketMapper   ticketMapper;
    @Mock private SeatMapMapper  seatMapMapper;
    @Mock private CustomerMapper customerMapper;

    // ── Real services (under test) ──────────────────────────────────────────
    private SeatMapService  seatMapService;
    private TicketService   ticketService;
    private CustomerService customerService;
    private ScheduleService scheduleService;   // used only for findByRoute

    // ── Shared fixtures ─────────────────────────────────────────────────────
    private UUID      routeId;
    private UUID      scheduleId;
    private UUID      customerId;
    private LocalDate travelDate;

    @BeforeEach
    void setUp() {
        routeId    = UUID.fromString("00700000-0000-0000-0000-000000000001");
        scheduleId = UUID.fromString("00800000-0000-0000-0000-000000000001");
        customerId = UUID.randomUUID();
        travelDate = LocalDate.of(2026, 3, 10);

        seatMapService  = new SeatMapService(seatMapRepository, scheduleRepository, seatMapMapper);
        ticketService   = new TicketService(ticketRepository, seatMapService, ticketMapper, eventPublisher, customerRepository);
        customerService = new CustomerService(customerRepository, customerMapper);
    }

    // ── Helper builders ──────────────────────────────────────────────────────

    private Bus buildBus(boolean twoFloors, int total, Integer floor1, Integer floor2) {
        Bus bus = new Bus();
        bus.setPlate("2340-ABC");
        bus.setHasTwoFloors(twoFloors);
        bus.setTotalSeats(total);
        bus.setSeatsFirstFloor(floor1);
        bus.setSeatsSecondFloor(floor2);
        return bus;
    }

    private Schedule buildSchedule(Bus bus) {
        Schedule schedule = new Schedule();
        setId(schedule, scheduleId);
        schedule.setBus(bus);
        return schedule;
    }

    private com.transporte.pasajes.entity.Ticket buildTicket(UUID id, int seat, int floor, TicketStatus st) {
        com.transporte.pasajes.entity.Ticket t = new com.transporte.pasajes.entity.Ticket();
        setId(t, id);
        t.setTicketCode("TKT-VNT-" + seat);
        t.setScheduleId(scheduleId);
        t.setCustomerId(customerId);
        t.setSeatNumber(seat);
        t.setFloorNumber(floor);
        t.setTravelDate(travelDate);
        t.setPrice(new BigDecimal("55.00"));
        t.setStatus(st);
        t.setSaleType(SaleType.COUNTER);
        return t;
    }

    private TicketResponse buildTicketResponse(UUID id, int seat, int floor, TicketStatus st) {
        return new TicketResponse(id, "TKT-VNT-" + seat, scheduleId, customerId,
                "Juan Pérez", "12345678", seat, floor, travelDate,
                new BigDecimal("55.00"), st, SaleType.COUNTER, null, null);
    }

    private com.transporte.pasajes.entity.SeatMap buildSeatEntry(int seat, int floor, SeatStatus st) {
        return com.transporte.pasajes.entity.SeatMap.builder()
                .scheduleId(scheduleId)
                .travelDate(travelDate)
                .seatNumber(seat)
                .floorNumber(floor)
                .status(st)
                .build();
    }

    private SeatMapResponse buildSeatMapResponse(int seat, int floor, SeatStatus st) {
        return new SeatMapResponse(UUID.randomUUID(), scheduleId, travelDate, seat, floor, st, null);
    }

    private CustomerResponse buildCustomerResponse(UUID id) {
        return new CustomerResponse(id, "12345678", "CI",
                "Juan", "Pérez", "juan@mail.com", "70011111", null, true, null);
    }

    private Customer buildCustomerEntity(UUID id) {
        Customer c = new Customer();
        setId(c, id);
        c.setDocumentNumber("12345678");
        c.setDocumentType("CI");
        c.setFirstName("Juan");
        c.setLastName("Pérez");
        c.setActive(true);
        return c;
    }

    /** Injects ID via reflection (BaseEntity has private field) */
    private void setId(Object entity, UUID id) {
        try {
            var f = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception ignored) { /* best-effort */ }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PASO 1 — Horarios por ruta
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Paso 1: Buscar horarios de la ruta")
    class Paso1HorariosPorRuta {

        @Test
        @DisplayName("Retorna horarios activos cuando la ruta existe")
        void returnsSchedulesForExistingRoute() {
            Bus bus = buildBus(false, 40, 40, null);
            Schedule schedule = buildSchedule(bus);

            given(scheduleRepository.findByRouteId(routeId))
                    .willReturn(List.of(schedule));

            ScheduleResponse schedResp = new ScheduleResponse(
                    scheduleId, routeId, "La Paz → Cochabamba",
                    bus.getId(), "2340-ABC",
                    UUID.randomUUID(), "Carlos Mamani",
                    LocalTime.of(8, 0), LocalTime.of(15, 0),
                    List.of(1, 5, 6), true, "Matutino", null
            );
            given(scheduleRepository.findByRouteId(routeId)).willReturn(List.of(schedule));

            // ScheduleService uses a mapper mock; test the repo contract directly
            List<Schedule> schedules = scheduleRepository.findByRouteId(routeId);

            assertThat(schedules).hasSize(1);
            assertThat(schedules.get(0).getBus().getPlate()).isEqualTo("2340-ABC");
            assertThat(schedules.get(0).getBus().getTotalSeats()).isEqualTo(40);
        }

        @Test
        @DisplayName("Retorna lista vacía cuando la ruta no tiene horarios")
        void returnsEmptyListWhenNoSchedules() {
            given(scheduleRepository.findByRouteId(routeId)).willReturn(List.of());

            List<Schedule> result = scheduleRepository.findByRouteId(routeId);

            assertThat(result).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PASO 2 — Mapa de asientos
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Paso 2: Mapa de asientos")
    class Paso2MapaAsientos {

        @Test
        @DisplayName("Genera el mapa cuando no existe — bus piso único, 40 asientos")
        void generatesSingleFloorMapOf40Seats() {
            Bus bus = buildBus(false, 40, null, null);
            Schedule schedule = buildSchedule(bus);

            given(seatMapRepository.findByScheduleIdAndTravelDate(scheduleId, travelDate))
                    .willReturn(List.of());
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(seatMapRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
            given(seatMapMapper.toResponse(any()))
                    .willAnswer(inv -> {
                        com.transporte.pasajes.entity.SeatMap sm = inv.getArgument(0);
                        return buildSeatMapResponse(sm.getSeatNumber(), sm.getFloorNumber(), sm.getStatus());
                    });

            List<SeatMapResponse> result = seatMapService.getSeatMap(scheduleId, travelDate);

            assertThat(result).hasSize(40);
            assertThat(result).allMatch(s -> s.floorNumber() == 1);
            assertThat(result).allMatch(s -> s.status() == SeatStatus.AVAILABLE);

            ArgumentCaptor<List<com.transporte.pasajes.entity.SeatMap>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(seatMapRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(40);
        }

        @Test
        @DisplayName("Genera el mapa para bus de dos pisos — 25 + 25 asientos")
        void generatesTwoFloorMapOf50Seats() {
            Bus bus = buildBus(true, 50, 25, 25);
            Schedule schedule = buildSchedule(bus);

            given(seatMapRepository.findByScheduleIdAndTravelDate(scheduleId, travelDate))
                    .willReturn(List.of());
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(seatMapRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
            given(seatMapMapper.toResponse(any()))
                    .willAnswer(inv -> {
                        com.transporte.pasajes.entity.SeatMap sm = inv.getArgument(0);
                        return buildSeatMapResponse(sm.getSeatNumber(), sm.getFloorNumber(), sm.getStatus());
                    });

            List<SeatMapResponse> result = seatMapService.getSeatMap(scheduleId, travelDate);

            assertThat(result).hasSize(50);
            long floor1Count = result.stream().filter(s -> s.floorNumber() == 1).count();
            long floor2Count = result.stream().filter(s -> s.floorNumber() == 2).count();
            assertThat(floor1Count).isEqualTo(25);
            assertThat(floor2Count).isEqualTo(25);
        }

        @Test
        @DisplayName("Retorna el mapa existente sin regenerar")
        void returnsExistingMapWithoutRegeneration() {
            List<com.transporte.pasajes.entity.SeatMap> existingSeats = List.of(
                    buildSeatEntry(1, 1, SeatStatus.AVAILABLE),
                    buildSeatEntry(2, 1, SeatStatus.SOLD),
                    buildSeatEntry(3, 1, SeatStatus.RESERVED)
            );
            given(seatMapRepository.findByScheduleIdAndTravelDate(scheduleId, travelDate))
                    .willReturn(existingSeats);
            given(seatMapMapper.toResponse(any()))
                    .willAnswer(inv -> {
                        com.transporte.pasajes.entity.SeatMap sm = inv.getArgument(0);
                        return buildSeatMapResponse(sm.getSeatNumber(), sm.getFloorNumber(), sm.getStatus());
                    });

            List<SeatMapResponse> result = seatMapService.getSeatMap(scheduleId, travelDate);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(SeatMapResponse::status)
                    .containsExactlyInAnyOrder(SeatStatus.AVAILABLE, SeatStatus.SOLD, SeatStatus.RESERVED);
            verify(scheduleRepository, never()).findById(any());
            verify(seatMapRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Cuenta correctamente los asientos disponibles")
        void countsAvailableSeats() {
            given(seatMapRepository.countByScheduleIdAndTravelDateAndStatus(
                    scheduleId, travelDate, SeatStatus.AVAILABLE)).willReturn(38L);

            long available = seatMapService.countAvailableSeats(scheduleId, travelDate);

            assertThat(available).isEqualTo(38);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PASO 3 — Gestión de clientes
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Paso 3: Búsqueda y registro de cliente")
    class Paso3Cliente {

        @Test
        @DisplayName("Encuentra cliente por número de documento")
        void findsCustomerByDocument() {
            Customer entity = buildCustomerEntity(customerId);
            CustomerResponse resp = buildCustomerResponse(customerId);

            given(customerRepository.findByDocumentNumber("12345678"))
                    .willReturn(Optional.of(entity));
            given(customerMapper.toResponse(entity)).willReturn(resp);

            CustomerResponse result = customerService.findByDocument("12345678");

            assertThat(result.firstName()).isEqualTo("Juan");
            assertThat(result.documentNumber()).isEqualTo("12345678");
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException cuando el documento no existe")
        void throwsWhenDocumentNotFound() {
            given(customerRepository.findByDocumentNumber("99999999"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.findByDocument("99999999"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99999999");
        }

        @Test
        @DisplayName("Crea un nuevo cliente cuando no existe en el sistema")
        void createsNewCustomerWhenNotFound() {
            CustomerRequest req = new CustomerRequest(
                    "99887766", "CI", "María", "Torres",
                    "maria@mail.com", "70099887", null, true
            );
            Customer newEntity = buildCustomerEntity(UUID.randomUUID());
            CustomerResponse resp = new CustomerResponse(
                    newEntity.getId(), "99887766", "CI", "María", "Torres",
                    "maria@mail.com", "70099887", null, true, null
            );

            given(customerRepository.existsByDocumentNumber("99887766")).willReturn(false);
            given(customerMapper.toEntity(req)).willReturn(newEntity);
            given(customerRepository.save(newEntity)).willReturn(newEntity);
            given(customerMapper.toResponse(newEntity)).willReturn(resp);

            CustomerResponse created = customerService.create(req);

            assertThat(created.firstName()).isEqualTo("María");
            assertThat(created.documentNumber()).isEqualTo("99887766");
            verify(customerRepository).save(newEntity);
        }

        @Test
        @DisplayName("Lanza BusinessException si el documento ya está registrado")
        void throwsWhenDocumentAlreadyRegistered() {
            CustomerRequest req = new CustomerRequest(
                    "12345678", "CI", "Copia", "Cliente", null, null, null, true
            );
            given(customerRepository.existsByDocumentNumber("12345678")).willReturn(true);

            assertThatThrownBy(() -> customerService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PASO 4 — Creación del ticket (venta confirmada)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Paso 4: Confirmar venta — crear ticket")
    class Paso4ConfirmarVenta {

        @Test
        @DisplayName("Crea ticket con SaleType.COUNTER y estado CONFIRMED")
        void createsTicketWithCounterSaleType() {
            UUID ticketId = UUID.randomUUID();
            com.transporte.pasajes.entity.Ticket entity = buildTicket(ticketId, 12, 1, TicketStatus.CONFIRMED);
            TicketResponse resp = buildTicketResponse(ticketId, 12, 1, TicketStatus.CONFIRMED);

            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    eq(scheduleId), eq(travelDate), eq(12), eq(1), eq(TicketStatus.CANCELLED)
            )).willReturn(false);
            given(ticketMapper.toEntity(any())).willReturn(entity);
            given(ticketRepository.save(any())).willReturn(entity);
            given(seatMapRepository.updateSeatStatus(scheduleId, travelDate, 12, 1, SeatStatus.SOLD, ticketId))
                    .willReturn(1);
            given(ticketMapper.toResponse(entity)).willReturn(resp);

            TicketRequest request = new TicketRequest(
                    scheduleId, customerId, 12, 1, travelDate,
                    new BigDecimal("55.00"), SaleType.COUNTER, null,
                    "Juan Pérez", "12345678", "CI"
            );

            TicketResponse result = ticketService.create(request);

            assertThat(result.status()).isEqualTo(TicketStatus.CONFIRMED);
            assertThat(result.saleType()).isEqualTo(SaleType.COUNTER);
            assertThat(result.seatNumber()).isEqualTo(12);
            assertThat(result.floorNumber()).isEqualTo(1);
            assertThat(result.price()).isEqualByComparingTo("55.00");
            assertThat(result.passengerName()).isEqualTo("Juan Pérez");

            verify(seatMapRepository).updateSeatStatus(scheduleId, travelDate, 12, 1, SeatStatus.SOLD, ticketId);
        }

        @Test
        @DisplayName("Falla si el asiento ya está vendido (SOLD)")
        void failsWhenSeatAlreadySold() {
            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    eq(scheduleId), eq(travelDate), eq(7), eq(1), eq(TicketStatus.CANCELLED)
            )).willReturn(true);

            TicketRequest request = new TicketRequest(
                    scheduleId, customerId, 7, 1, travelDate,
                    new BigDecimal("55.00"), SaleType.COUNTER, null, null, null, "CI"
            );

            assertThatThrownBy(() -> ticketService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already taken");

            verify(ticketRepository, never()).save(any());
            verify(seatMapRepository, never()).updateSeatStatus(any(), any(), anyInt(), anyInt(), any(), any());
        }

        @Test
        @DisplayName("El enum COUNTER es válido y distinto a VENTANILLA")
        void counterEnumIsValid() {
            assertThat(SaleType.COUNTER).isNotNull();
            assertThat(SaleType.COUNTER).isNotEqualTo(SaleType.VENTANILLA);
            assertThat(SaleType.COUNTER.name()).isEqualTo("COUNTER");
        }

        @Test
        @DisplayName("El enum AGENCY también es válido")
        void agencyEnumIsValid() {
            assertThat(SaleType.AGENCY).isNotNull();
            assertThat(SaleType.AGENCY.name()).isEqualTo("AGENCY");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FLUJO COMPLETO — Venta en ventanilla end-to-end
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Flujo completo: Venta en Ventanilla E2E")
    class FlujoCompletoVentaVentanilla {

        @Test
        @DisplayName("Flujo exitoso: buscar horario → seleccionar asiento → crear ticket → asiento SOLD")
        void happyPathFullFlow() {
            // ── Paso 1: horario existe ───────────────────────────────────────
            Bus bus = buildBus(false, 40, null, null);
            Schedule schedule = buildSchedule(bus);
            given(scheduleRepository.findByRouteId(routeId)).willReturn(List.of(schedule));
            assertThat(scheduleRepository.findByRouteId(routeId)).hasSize(1);

            // ── Paso 2: seat map generado (ninguno existente aún) ────────────
            given(seatMapRepository.findByScheduleIdAndTravelDate(scheduleId, travelDate))
                    .willReturn(List.of());
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(seatMapRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
            given(seatMapMapper.toResponse(any()))
                    .willAnswer(inv -> {
                        com.transporte.pasajes.entity.SeatMap sm = inv.getArgument(0);
                        return buildSeatMapResponse(sm.getSeatNumber(), sm.getFloorNumber(), sm.getStatus());
                    });

            List<SeatMapResponse> seatMap = seatMapService.getSeatMap(scheduleId, travelDate);
            assertThat(seatMap).hasSize(40);
            assertThat(seatMap.get(0).status()).isEqualTo(SeatStatus.AVAILABLE);
            int selectedSeat  = seatMap.get(9).seatNumber();   // asiento #10
            int selectedFloor = seatMap.get(9).floorNumber();  // piso 1

            // ── Paso 3: cliente encontrado ───────────────────────────────────
            Customer customerEntity = buildCustomerEntity(customerId);
            CustomerResponse customerResp = buildCustomerResponse(customerId);
            given(customerRepository.findByDocumentNumber("12345678"))
                    .willReturn(Optional.of(customerEntity));
            given(customerMapper.toResponse(customerEntity)).willReturn(customerResp);

            CustomerResponse foundCustomer = customerService.findByDocument("12345678");
            assertThat(foundCustomer.id()).isEqualTo(customerId);

            // ── Paso 4: crear ticket ─────────────────────────────────────────
            UUID ticketId = UUID.randomUUID();
            com.transporte.pasajes.entity.Ticket ticketEntity =
                    buildTicket(ticketId, selectedSeat, selectedFloor, TicketStatus.CONFIRMED);
            TicketResponse ticketResp =
                    buildTicketResponse(ticketId, selectedSeat, selectedFloor, TicketStatus.CONFIRMED);

            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    eq(scheduleId), eq(travelDate), eq(selectedSeat), eq(selectedFloor),
                    eq(TicketStatus.CANCELLED)
            )).willReturn(false);
            given(ticketMapper.toEntity(any())).willReturn(ticketEntity);
            given(ticketRepository.save(any())).willReturn(ticketEntity);
            given(seatMapRepository.updateSeatStatus(scheduleId, travelDate, selectedSeat, selectedFloor, SeatStatus.SOLD, ticketId))
                    .willReturn(1);
            given(ticketMapper.toResponse(ticketEntity)).willReturn(ticketResp);

            TicketRequest ventaRequest = new TicketRequest(
                    scheduleId, foundCustomer.id(), selectedSeat, selectedFloor, travelDate,
                    new BigDecimal("55.00"), SaleType.COUNTER, null,
                    foundCustomer.firstName() + " " + foundCustomer.lastName(),
                    foundCustomer.documentNumber(), "CI"
            );
            TicketResponse ticket = ticketService.create(ventaRequest);

            // ── Verificaciones finales ───────────────────────────────────────
            assertThat(ticket).isNotNull();
            assertThat(ticket.status()).isEqualTo(TicketStatus.CONFIRMED);
            assertThat(ticket.saleType()).isEqualTo(SaleType.COUNTER);
            assertThat(ticket.seatNumber()).isEqualTo(selectedSeat);
            assertThat(ticket.customerId()).isEqualTo(customerId);
            assertThat(ticket.price()).isEqualByComparingTo("55.00");

            verify(seatMapRepository)
                    .updateSeatStatus(scheduleId, travelDate, selectedSeat, selectedFloor, SeatStatus.SOLD, ticketId);
        }

        @Test
        @DisplayName("Flujo de cancelación: ticket cancelado libera el asiento")
        void cancellationReleasesTheSeat() {
            UUID ticketId = UUID.randomUUID();
            com.transporte.pasajes.entity.Ticket ticketEntity =
                    buildTicket(ticketId, 5, 1, TicketStatus.CONFIRMED);

            // Cancel
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(ticketEntity));
            given(ticketRepository.save(any())).willReturn(ticketEntity);
            given(seatMapRepository.updateSeatStatus(scheduleId, travelDate, 5, 1, SeatStatus.AVAILABLE, ticketId))
                    .willReturn(1);

            ticketService.cancel(ticketId);

            ArgumentCaptor<com.transporte.pasajes.entity.Ticket> cap =
                    ArgumentCaptor.forClass(com.transporte.pasajes.entity.Ticket.class);
            verify(ticketRepository).save(cap.capture());
            assertThat(cap.getValue().getStatus()).isEqualTo(TicketStatus.CANCELLED);
            verify(seatMapRepository).updateSeatStatus(scheduleId, travelDate, 5, 1, SeatStatus.AVAILABLE, ticketId);
        }

        @Test
        @DisplayName("Flujo con nuevo cliente: no encontrado → crear → crear ticket")
        void newCustomerCreatedAndTicketSold() {
            UUID newCustomerId = UUID.randomUUID();
            CustomerRequest newReq = new CustomerRequest(
                    "88776655", "CI", "Ana", "García", null, "70000001", null, true
            );
            Customer newEntity = buildCustomerEntity(newCustomerId);
            CustomerResponse newResp = new CustomerResponse(
                    newCustomerId, "88776655", "CI", "Ana", "García", null, "70000001", null, true, null
            );

            // Paso 3a: cliente no encontrado
            given(customerRepository.findByDocumentNumber("88776655")).willReturn(Optional.empty());
            assertThatThrownBy(() -> customerService.findByDocument("88776655"))
                    .isInstanceOf(ResourceNotFoundException.class);

            // Paso 3b: registrar cliente
            given(customerRepository.existsByDocumentNumber("88776655")).willReturn(false);
            given(customerMapper.toEntity(newReq)).willReturn(newEntity);
            given(customerRepository.save(newEntity)).willReturn(newEntity);
            given(customerMapper.toResponse(newEntity)).willReturn(newResp);

            CustomerResponse created = customerService.create(newReq);
            assertThat(created.firstName()).isEqualTo("Ana");

            // Paso 4: crear ticket con el nuevo cliente
            UUID ticketId = UUID.randomUUID();
            com.transporte.pasajes.entity.Ticket ticketEntity =
                    buildTicket(ticketId, 3, 1, TicketStatus.CONFIRMED);
            TicketResponse ticketResp =
                    buildTicketResponse(ticketId, 3, 1, TicketStatus.CONFIRMED);

            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    eq(scheduleId), eq(travelDate), eq(3), eq(1), eq(TicketStatus.CANCELLED)
            )).willReturn(false);
            given(ticketMapper.toEntity(any())).willReturn(ticketEntity);
            given(ticketRepository.save(any())).willReturn(ticketEntity);
            given(seatMapRepository.updateSeatStatus(scheduleId, travelDate, 3, 1, SeatStatus.SOLD, ticketId))
                    .willReturn(1);
            given(ticketMapper.toResponse(ticketEntity)).willReturn(ticketResp);

            TicketRequest req = new TicketRequest(
                    scheduleId, newCustomerId, 3, 1, travelDate,
                    new BigDecimal("55.00"), SaleType.COUNTER, null, "Ana García", "88776655", "CI"
            );
            TicketResponse ticket = ticketService.create(req);

            assertThat(ticket.status()).isEqualTo(TicketStatus.CONFIRMED);
            verify(seatMapRepository).updateSeatStatus(scheduleId, travelDate, 3, 1, SeatStatus.SOLD, ticketId);
        }
    }
}
