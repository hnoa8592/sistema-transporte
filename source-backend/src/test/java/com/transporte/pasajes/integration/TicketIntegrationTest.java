package com.transporte.pasajes.integration;

import com.transporte.core.exception.BusinessException;
import com.transporte.operacion.entity.Bus;
import com.transporte.operacion.entity.Customer;
import com.transporte.operacion.entity.Schedule;
import com.transporte.operacion.repository.CustomerRepository;
import com.transporte.operacion.repository.ScheduleRepository;
import com.transporte.pasajes.dto.BulkTicketRequest;
import com.transporte.pasajes.dto.ChangeSeatRequest;
import com.transporte.pasajes.dto.SeatMapResponse;
import com.transporte.pasajes.dto.TicketRequest;
import com.transporte.pasajes.dto.TicketResponse;
import com.transporte.pasajes.enums.SaleType;
import com.transporte.pasajes.enums.SeatStatus;
import com.transporte.pasajes.enums.TicketStatus;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

/**
 * Integration-style test for the ticket flow.
 *
 * Because the pasajes module depends on the operacion module (for Schedule/Bus),
 * and running a full @SpringBootTest with H2 would require configuring all
 * cross-module entities, this test takes a pragmatic approach: it wires the real
 * TicketService and SeatMapService together while mocking only the external
 * ScheduleRepository (operacion module) and the database repositories.
 *
 * This validates the end-to-end business logic between the two services without
 * requiring a full application context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Ticket Flow Integration Tests")
class TicketIntegrationTest {

    // Repository mocks (in a full integration test these would be real JPA repos with H2)
    @Mock private TicketRepository ticketRepository;
    @Mock private SeatMapRepository seatMapRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CustomerRepository customerRepository;

    // Real mappers are wired manually below
    private com.transporte.pasajes.mapper.TicketMapper ticketMapper;
    private com.transporte.pasajes.mapper.SeatMapMapper seatMapMapper;

    private SeatMapService seatMapService;
    private TicketService ticketService;

    private UUID scheduleId;
    private LocalDate travelDate;

    @BeforeEach
    void setUp() {
        scheduleId = UUID.randomUUID();
        travelDate = LocalDate.now().plusDays(5);

        // Use Mockito spies for mappers to keep them lightweight
        ticketMapper = org.mockito.Mockito.mock(com.transporte.pasajes.mapper.TicketMapper.class);
        seatMapMapper = org.mockito.Mockito.mock(com.transporte.pasajes.mapper.SeatMapMapper.class);

        seatMapService = new SeatMapService(seatMapRepository, scheduleRepository, seatMapMapper);
        ticketService = new TicketService(ticketRepository, seatMapService, ticketMapper, eventPublisher, customerRepository);
    }

    /**
     * Builds a mock Bus configured as single-floor with given seat count.
     */
    private Bus buildBus(boolean twoFloors, int totalSeats, Integer floor1, Integer floor2) {
        Bus bus = new Bus();
        bus.setPlate("INT-001");
        bus.setHasTwoFloors(twoFloors);
        bus.setTotalSeats(totalSeats);
        bus.setSeatsFirstFloor(floor1);
        bus.setSeatsSecondFloor(floor2);
        return bus;
    }

    private Schedule buildSchedule(Bus bus) {
        Schedule schedule = new Schedule();
        try {
            var idField = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(schedule, scheduleId);
        } catch (Exception ignored) {}
        schedule.setBus(bus);
        return schedule;
    }

    private com.transporte.pasajes.entity.Ticket buildPersistedTicket(UUID id, int seatNum, int floor, TicketStatus status) {
        com.transporte.pasajes.entity.Ticket ticket = new com.transporte.pasajes.entity.Ticket();
        try {
            var idField = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(ticket, id);
        } catch (Exception ignored) {}
        ticket.setTicketCode("TKT-INT-" + seatNum);
        ticket.setScheduleId(scheduleId);
        ticket.setCustomerId(UUID.randomUUID());
        ticket.setSeatNumber(seatNum);
        ticket.setFloorNumber(floor);
        ticket.setTravelDate(travelDate);
        ticket.setPrice(new BigDecimal("120.00"));
        ticket.setStatus(status);
        ticket.setSaleType(SaleType.VENTANILLA);
        return ticket;
    }

    private TicketResponse buildResponse(UUID id, int seatNum, int floor, TicketStatus status) {
        return new TicketResponse(id, "TKT-INT-" + seatNum, scheduleId, UUID.randomUUID(),
                null, null, seatNum, floor, travelDate, new BigDecimal("120.00"),
                status, SaleType.VENTANILLA, null, null);
    }

    @Nested
    @DisplayName("Complete ticket lifecycle flow")
    class TicketLifecycleFlow {

        @Test
        @DisplayName("Flow 1: Create ticket -> verify seat SOLD -> cancel -> seat back to AVAILABLE")
        void createAndCancelTicketFlow() {
            UUID ticketId = UUID.randomUUID();
            com.transporte.pasajes.entity.Ticket persistedTicket = buildPersistedTicket(ticketId, 5, 1, TicketStatus.CONFIRMED);
            TicketResponse response = buildResponse(ticketId, 5, 1, TicketStatus.CONFIRMED);

            // Step 1: Seat is available, ticket is created
            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    eq(scheduleId), eq(travelDate), eq(5), eq(1), eq(TicketStatus.CANCELLED)
            )).willReturn(false);
            given(ticketMapper.toEntity(any())).willReturn(persistedTicket);
            given(ticketRepository.save(any())).willReturn(persistedTicket);
            given(seatMapRepository.updateSeatStatus(scheduleId, travelDate, 5, 1, SeatStatus.SOLD, ticketId)).willReturn(1);
            given(ticketMapper.toResponse(persistedTicket)).willReturn(response);

            TicketRequest createRequest = new TicketRequest(
                    scheduleId, UUID.randomUUID(), 5, 1, travelDate,
                    new BigDecimal("120.00"), SaleType.VENTANILLA, null, null, null, null
            );
            TicketResponse created = ticketService.create(createRequest);

            assertThat(created).isNotNull();
            assertThat(created.status()).isEqualTo(TicketStatus.CONFIRMED);
            assertThat(created.seatNumber()).isEqualTo(5);

            // Seat marked as SOLD
            verify(seatMapRepository).updateSeatStatus(scheduleId, travelDate, 5, 1, SeatStatus.SOLD, ticketId);

            // Step 2: Cancel the ticket -> seat released to AVAILABLE
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(persistedTicket));
            given(ticketRepository.save(any())).willReturn(persistedTicket);
            given(seatMapRepository.updateSeatStatus(scheduleId, travelDate, 5, 1, SeatStatus.AVAILABLE, ticketId)).willReturn(1);

            ticketService.cancel(ticketId);

            // Verify ticket now cancelled
            org.mockito.ArgumentCaptor<com.transporte.pasajes.entity.Ticket> captor =
                    org.mockito.ArgumentCaptor.forClass(com.transporte.pasajes.entity.Ticket.class);
            verify(ticketRepository, times(2)).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.CANCELLED);

            // Seat released to AVAILABLE
            verify(seatMapRepository).updateSeatStatus(scheduleId, travelDate, 5, 1, SeatStatus.AVAILABLE, ticketId);
        }

        @Test
        @DisplayName("Flow 2: Create ticket -> attempt duplicate seat -> must fail")
        void duplicateSeatShouldFail() {
            UUID ticketId = UUID.randomUUID();
            com.transporte.pasajes.entity.Ticket persistedTicket = buildPersistedTicket(ticketId, 3, 1, TicketStatus.CONFIRMED);
            TicketResponse response = buildResponse(ticketId, 3, 1, TicketStatus.CONFIRMED);

            // First creation succeeds
            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    eq(scheduleId), eq(travelDate), eq(3), eq(1), eq(TicketStatus.CANCELLED)
            )).willReturn(false);
            given(ticketMapper.toEntity(any())).willReturn(persistedTicket);
            given(ticketRepository.save(any())).willReturn(persistedTicket);
            given(seatMapRepository.updateSeatStatus(scheduleId, travelDate, 3, 1, SeatStatus.SOLD, ticketId)).willReturn(1);
            given(ticketMapper.toResponse(persistedTicket)).willReturn(response);

            TicketRequest firstRequest = new TicketRequest(
                    scheduleId, UUID.randomUUID(), 3, 1, travelDate,
                    new BigDecimal("120.00"), SaleType.VENTANILLA, null, null, null, null
            );
            TicketResponse firstTicket = ticketService.create(firstRequest);
            assertThat(firstTicket.seatNumber()).isEqualTo(3);

            // Second attempt on same seat -> fails
            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    eq(scheduleId), eq(travelDate), eq(3), eq(1), eq(TicketStatus.CANCELLED)
            )).willReturn(true);

            TicketRequest duplicateRequest = new TicketRequest(
                    scheduleId, UUID.randomUUID(), 3, 1, travelDate,
                    new BigDecimal("120.00"), SaleType.VENTANILLA, null, null, null, null
            );

            assertThatThrownBy(() -> ticketService.create(duplicateRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya está ocupado");
        }

        @Test
        @DisplayName("Flow 3: Create ticket -> change seat -> old seat AVAILABLE, new seat SOLD")
        void changeSeatFlow() {
            UUID ticketId = UUID.randomUUID();
            com.transporte.pasajes.entity.Ticket persistedTicket = buildPersistedTicket(ticketId, 1, 1, TicketStatus.CONFIRMED);
            TicketResponse responseAfterCreate = buildResponse(ticketId, 1, 1, TicketStatus.CONFIRMED);
            TicketResponse responseAfterChange = buildResponse(ticketId, 15, 2, TicketStatus.CONFIRMED);

            // Create ticket on seat 1, floor 1
            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    eq(scheduleId), eq(travelDate), eq(1), eq(1), eq(TicketStatus.CANCELLED)
            )).willReturn(false);
            given(ticketMapper.toEntity(any())).willReturn(persistedTicket);
            given(ticketRepository.save(any())).willReturn(persistedTicket);
            given(seatMapRepository.updateSeatStatus(scheduleId, travelDate, 1, 1, SeatStatus.SOLD, ticketId)).willReturn(1);
            given(ticketMapper.toResponse(persistedTicket)).willReturn(responseAfterCreate);

            TicketRequest createRequest = new TicketRequest(
                    scheduleId, UUID.randomUUID(), 1, 1, travelDate,
                    new BigDecimal("120.00"), SaleType.VENTANILLA, null, null, null, null
            );
            ticketService.create(createRequest);

            // Change to seat 15, floor 2
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(persistedTicket));
            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    eq(scheduleId), eq(travelDate), eq(15), eq(2), eq(TicketStatus.CANCELLED)
            )).willReturn(false);
            given(seatMapRepository.updateSeatStatus(scheduleId, travelDate, 1, 1, SeatStatus.AVAILABLE, ticketId)).willReturn(1);
            given(seatMapRepository.updateSeatStatus(scheduleId, travelDate, 15, 2, SeatStatus.SOLD, ticketId)).willReturn(1);
            given(ticketMapper.toResponse(any())).willReturn(responseAfterChange);

            ChangeSeatRequest changeSeatRequest = new ChangeSeatRequest(15, 2);
            ticketService.changeSeat(ticketId, changeSeatRequest);

            // Old seat released
            verify(seatMapRepository).updateSeatStatus(scheduleId, travelDate, 1, 1, SeatStatus.AVAILABLE, ticketId);
            // New seat blocked
            verify(seatMapRepository).updateSeatStatus(scheduleId, travelDate, 15, 2, SeatStatus.SOLD, ticketId);
        }

        @Test
        @DisplayName("Flow 4: Cancel already-cancelled ticket must throw BusinessException")
        void cancellingAlreadyCancelledTicketFails() {
            UUID ticketId = UUID.randomUUID();
            com.transporte.pasajes.entity.Ticket cancelledTicket = buildPersistedTicket(ticketId, 8, 1, TicketStatus.CANCELLED);

            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(cancelledTicket));

            assertThatThrownBy(() -> ticketService.cancel(ticketId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya está cancelado");

            // Seat map should NOT be touched
            verify(seatMapRepository, never()).updateSeatStatus(any(), any(), anyInt(), anyInt(), any(), any());
        }
    }

    @Nested
    @DisplayName("SeatMap generation flow")
    class SeatMapGenerationFlow {

        @Test
        @DisplayName("Should generate seat map from schedule when none exists")
        void shouldGenerateSeatMapFromSchedule() {
            Bus bus = buildBus(false, 10, null, null);
            Schedule schedule = buildSchedule(bus);

            given(seatMapRepository.findByScheduleIdAndTravelDate(scheduleId, travelDate))
                    .willReturn(List.of());
            given(scheduleRepository.findById(scheduleId)).willReturn(Optional.of(schedule));
            given(seatMapRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));
            given(seatMapMapper.toResponse(any())).willReturn(
                    new SeatMapResponse(UUID.randomUUID(), scheduleId, travelDate, 1, 1, SeatStatus.AVAILABLE, null)
            );

            List<SeatMapResponse> result = seatMapService.getSeatMap(scheduleId, travelDate);

            assertThat(result).hasSize(10);
            verify(scheduleRepository).findById(scheduleId);
            verify(seatMapRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should return existing seat map without regenerating")
        void shouldReturnExistingWithoutRegeneration() {
            com.transporte.pasajes.entity.SeatMap existingSeat = com.transporte.pasajes.entity.SeatMap.builder()
                    .scheduleId(scheduleId)
                    .travelDate(travelDate)
                    .seatNumber(1)
                    .floorNumber(1)
                    .status(SeatStatus.AVAILABLE)
                    .build();

            given(seatMapRepository.findByScheduleIdAndTravelDate(scheduleId, travelDate))
                    .willReturn(List.of(existingSeat));
            given(seatMapMapper.toResponse(existingSeat)).willReturn(
                    new SeatMapResponse(UUID.randomUUID(), scheduleId, travelDate, 1, 1, SeatStatus.AVAILABLE, null)
            );

            List<SeatMapResponse> result = seatMapService.getSeatMap(scheduleId, travelDate);

            assertThat(result).hasSize(1);
            // Should NOT hit the schedule repository
            verify(scheduleRepository, never()).findById(any());
            verify(seatMapRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("Bulk ticket sale — auto customer registration")
    class BulkCustomerRegistrationFlow {

        private void stubTicketCreation(int seatNum, com.transporte.pasajes.entity.Ticket persistedTicket, TicketResponse response) {
            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    eq(scheduleId), eq(travelDate), eq(seatNum), anyInt(), eq(TicketStatus.CANCELLED)
            )).willReturn(false);
            given(ticketMapper.toEntity(argThat(r -> r != null && r.seatNumber() == seatNum))).willReturn(persistedTicket);
            given(ticketMapper.toResponse(persistedTicket)).willReturn(response);
            given(seatMapRepository.updateSeatStatus(eq(scheduleId), eq(travelDate), eq(seatNum), anyInt(), eq(SeatStatus.SOLD), any(UUID.class))).willReturn(1);
        }

        private Customer buildSavedCustomer(UUID id, String document) {
            Customer c = new Customer();
            try {
                var idField = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(c, id);
            } catch (Exception ignored) {}
            c.setDocumentNumber(document);
            return c;
        }

        @Test
        @DisplayName("New customer is registered when passengerDocument provided without customerId")
        void newCustomerIsRegisteredDuringBulkSale() {
            String document = "12345678";
            UUID newCustomerId = UUID.randomUUID();
            Customer saved = buildSavedCustomer(newCustomerId, document);

            given(customerRepository.findByDocumentNumber(document)).willReturn(Optional.empty());
            given(customerRepository.save(any(Customer.class))).willReturn(saved);

            com.transporte.pasajes.entity.Ticket ticket = buildPersistedTicket(UUID.randomUUID(), 5, 1, TicketStatus.CONFIRMED);
            TicketResponse response = buildResponse(UUID.randomUUID(), 5, 1, TicketStatus.CONFIRMED);
            stubTicketCreation(5, ticket, response);
            given(ticketRepository.save(ticket)).willReturn(ticket);

            BulkTicketRequest bulk = new BulkTicketRequest(List.of(
                    new TicketRequest(scheduleId, null, 5, 1, travelDate,
                            new BigDecimal("120.00"), SaleType.VENTANILLA, null, "Juan Perez", document, null)
            ));

            List<TicketResponse> results = ticketService.createBulk(bulk);

            assertThat(results).hasSize(1);
            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            assertThat(captor.getValue().getDocumentNumber()).isEqualTo(document);
            assertThat(captor.getValue().getFirstName()).isEqualTo("Juan");
            assertThat(captor.getValue().getLastName()).isEqualTo("Perez");
        }

        @Test
        @DisplayName("Existing customer is reused — no new row created")
        void existingCustomerIsReusedDuringBulkSale() {
            String document = "87654321";
            UUID existingId = UUID.randomUUID();
            Customer existing = buildSavedCustomer(existingId, document);

            given(customerRepository.findByDocumentNumber(document)).willReturn(Optional.of(existing));

            com.transporte.pasajes.entity.Ticket ticket = buildPersistedTicket(UUID.randomUUID(), 7, 1, TicketStatus.CONFIRMED);
            TicketResponse response = buildResponse(UUID.randomUUID(), 7, 1, TicketStatus.CONFIRMED);
            stubTicketCreation(7, ticket, response);
            given(ticketRepository.save(ticket)).willReturn(ticket);

            BulkTicketRequest bulk = new BulkTicketRequest(List.of(
                    new TicketRequest(scheduleId, null, 7, 1, travelDate,
                            new BigDecimal("120.00"), SaleType.VENTANILLA, null, "Maria Lopez", document, null)
            ));

            ticketService.createBulk(bulk);

            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        @DisplayName("Ticket with existing customerId skips customer resolution entirely")
        void ticketWithCustomerIdSkipsRegistration() {
            UUID existingCustomerId = UUID.randomUUID();

            com.transporte.pasajes.entity.Ticket ticket = buildPersistedTicket(UUID.randomUUID(), 3, 1, TicketStatus.CONFIRMED);
            TicketResponse response = buildResponse(UUID.randomUUID(), 3, 1, TicketStatus.CONFIRMED);
            stubTicketCreation(3, ticket, response);
            given(ticketRepository.save(ticket)).willReturn(ticket);

            BulkTicketRequest bulk = new BulkTicketRequest(List.of(
                    new TicketRequest(scheduleId, existingCustomerId, 3, 1, travelDate,
                            new BigDecimal("120.00"), SaleType.VENTANILLA, null, null, null, null)
            ));

            ticketService.createBulk(bulk);

            verifyNoInteractions(customerRepository);
        }

        @Test
        @DisplayName("Ticket without document skips registration even when no customerId")
        void ticketWithoutDocumentSkipsRegistration() {
            com.transporte.pasajes.entity.Ticket ticket = buildPersistedTicket(UUID.randomUUID(), 2, 1, TicketStatus.CONFIRMED);
            TicketResponse response = buildResponse(UUID.randomUUID(), 2, 1, TicketStatus.CONFIRMED);
            stubTicketCreation(2, ticket, response);
            given(ticketRepository.save(ticket)).willReturn(ticket);

            BulkTicketRequest bulk = new BulkTicketRequest(List.of(
                    new TicketRequest(scheduleId, null, 2, 1, travelDate,
                            new BigDecimal("120.00"), SaleType.VENTANILLA, null, null, null, null)
            ));

            ticketService.createBulk(bulk);

            verifyNoInteractions(customerRepository);
        }

        @Test
        @DisplayName("Mixed bulk: only new customers are registered, existing ones reused")
        void mixedBulkRegistersOnlyNewCustomers() {
            String newDoc = "11111111";
            UUID newCustomerId = UUID.randomUUID();
            UUID preExistingId = UUID.randomUUID();
            Customer savedCustomer = buildSavedCustomer(newCustomerId, newDoc);

            given(customerRepository.findByDocumentNumber(newDoc)).willReturn(Optional.empty());
            given(customerRepository.save(any(Customer.class))).willReturn(savedCustomer);

            com.transporte.pasajes.entity.Ticket ticket1 = buildPersistedTicket(UUID.randomUUID(), 1, 1, TicketStatus.CONFIRMED);
            com.transporte.pasajes.entity.Ticket ticket2 = buildPersistedTicket(UUID.randomUUID(), 2, 1, TicketStatus.CONFIRMED);
            TicketResponse resp1 = buildResponse(UUID.randomUUID(), 1, 1, TicketStatus.CONFIRMED);
            TicketResponse resp2 = buildResponse(UUID.randomUUID(), 2, 1, TicketStatus.CONFIRMED);

            stubTicketCreation(1, ticket1, resp1);
            stubTicketCreation(2, ticket2, resp2);
            given(ticketRepository.save(ticket1)).willReturn(ticket1);
            given(ticketRepository.save(ticket2)).willReturn(ticket2);

            BulkTicketRequest bulk = new BulkTicketRequest(List.of(
                    new TicketRequest(scheduleId, null, 1, 1, travelDate,
                            new BigDecimal("120.00"), SaleType.VENTANILLA, null, "Carlos Ruiz", newDoc, "CI"),
                    new TicketRequest(scheduleId, preExistingId, 2, 1, travelDate,
                            new BigDecimal("120.00"), SaleType.VENTANILLA, null, null, null, "CI")
            ));

            List<TicketResponse> results = ticketService.createBulk(bulk);

            assertThat(results).hasSize(2);
            verify(customerRepository, times(1)).save(any(Customer.class));
        }
    }
}
