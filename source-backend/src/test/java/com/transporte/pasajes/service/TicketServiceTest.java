package com.transporte.pasajes.service;

import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.pasajes.dto.ChangeSeatRequest;
import com.transporte.pasajes.dto.TicketRequest;
import com.transporte.pasajes.dto.TicketResponse;
import com.transporte.pasajes.dto.UpdateTicketCustomerRequest;
import com.transporte.pasajes.entity.Ticket;
import com.transporte.pasajes.enums.SaleType;
import com.transporte.pasajes.enums.SeatStatus;
import com.transporte.pasajes.enums.TicketStatus;
import com.transporte.pasajes.mapper.TicketMapper;
import com.transporte.pasajes.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService Unit Tests")
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private SeatMapService seatMapService;
    @Mock private TicketMapper ticketMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TicketService ticketService;

    private UUID scheduleId;
    private UUID ticketId;
    private UUID customerId;
    private LocalDate travelDate;
    private Ticket confirmedTicket;
    private TicketResponse ticketResponse;

    @BeforeEach
    void setUp() {
        scheduleId = UUID.randomUUID();
        ticketId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        travelDate = LocalDate.now().plusDays(3);

        confirmedTicket = buildTicket(ticketId, TicketStatus.CONFIRMED);
        ticketResponse = buildTicketResponse(ticketId, TicketStatus.CONFIRMED);
    }

    private Ticket buildTicket(UUID id, TicketStatus status) {
        Ticket ticket = new Ticket();
        try {
            var idField = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(ticket, id);
        } catch (Exception e) {
            // ignore for test
        }
        ticket.setTicketCode("TKT-TEST001");
        ticket.setScheduleId(scheduleId);
        ticket.setCustomerId(customerId);
        ticket.setSeatNumber(5);
        ticket.setFloorNumber(1);
        ticket.setTravelDate(travelDate);
        ticket.setPrice(new BigDecimal("150.00"));
        ticket.setStatus(status);
        ticket.setSaleType(SaleType.VENTANILLA);
        return ticket;
    }

    private TicketResponse buildTicketResponse(UUID id, TicketStatus status) {
        return new TicketResponse(id, "TKT-TEST001", scheduleId, customerId,
                null, null, 5, 1, travelDate, new BigDecimal("150.00"),
                status, SaleType.VENTANILLA, null, null);
    }

    @Nested
    @DisplayName("create() tests")
    class CreateTests {

        @Test
        @DisplayName("Should create ticket when seat is available")
        void shouldCreateTicketWhenSeatIsAvailable() {
            TicketRequest request = new TicketRequest(
                    scheduleId, customerId, 5, 1, travelDate,
                    new BigDecimal("150.00"), SaleType.VENTANILLA, null, "Nombre prueba", "741852", "CI"
            );

            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    any(), any(), anyInt(), anyInt(), any())).willReturn(false);
            given(ticketMapper.toEntity(request)).willReturn(confirmedTicket);
            given(ticketRepository.save(any())).willReturn(confirmedTicket);
            given(ticketMapper.toResponse(confirmedTicket)).willReturn(ticketResponse);

            TicketResponse result = ticketService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(TicketStatus.CONFIRMED);
            verify(seatMapService).updateSeatStatus(
                    eq(scheduleId), eq(travelDate), eq(5), eq(1), eq(SeatStatus.SOLD), any(UUID.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when seat is already taken")
        void shouldThrowExceptionWhenSeatAlreadyTaken() {
            TicketRequest request = new TicketRequest(
                    scheduleId, customerId, 5, 1, travelDate,
                    new BigDecimal("150.00"), SaleType.VENTANILLA, null, null, null, "CI"
            );

            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    any(), any(), anyInt(), anyInt(), any())).willReturn(true);

            assertThatThrownBy(() -> ticketService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya está ocupado");

            verify(ticketRepository, never()).save(any());
            verify(seatMapService, never()).updateSeatStatus(any(), any(), anyInt(), anyInt(), any(), any());
        }

        @Test
        @DisplayName("Should set ticket status to CONFIRMED on creation")
        void shouldSetStatusToConfirmedOnCreation() {
            TicketRequest request = new TicketRequest(
                    scheduleId, customerId, 5, 1, travelDate,
                    new BigDecimal("150.00"), SaleType.VENTANILLA, null, null, null, "CI"
            );

            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    any(), any(), anyInt(), anyInt(), any())).willReturn(false);
            given(ticketMapper.toEntity(request)).willReturn(confirmedTicket);
            given(ticketRepository.save(any())).willReturn(confirmedTicket);
            given(ticketMapper.toResponse(confirmedTicket)).willReturn(ticketResponse);

            ticketService.create(request);

            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Should default saleType to VENTANILLA when not specified")
        void shouldDefaultSaleTypeToVentanilla() {
            TicketRequest request = new TicketRequest(
                    scheduleId, customerId, 5, 1, travelDate,
                    new BigDecimal("150.00"), null, null, null, null, "CI"
            );
            Ticket ticketWithNullSaleType = buildTicket(ticketId, TicketStatus.CONFIRMED);
            ticketWithNullSaleType.setSaleType(null);

            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    any(), any(), anyInt(), anyInt(), any())).willReturn(false);
            given(ticketMapper.toEntity(request)).willReturn(ticketWithNullSaleType);
            given(ticketRepository.save(any())).willReturn(confirmedTicket);
            given(ticketMapper.toResponse(any())).willReturn(ticketResponse);

            ticketService.create(request);

            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(captor.capture());
            assertThat(captor.getValue().getSaleType()).isEqualTo(SaleType.VENTANILLA);
        }
    }

    @Nested
    @DisplayName("cancel() tests")
    class CancelTests {

        @Test
        @DisplayName("Should cancel ticket and release seat")
        void shouldCancelTicketAndReleaseSeat() {
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(confirmedTicket));
            given(ticketRepository.save(any())).willReturn(confirmedTicket);

            ticketService.cancel(ticketId);

            ArgumentCaptor<Ticket> ticketCaptor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(ticketCaptor.capture());
            assertThat(ticketCaptor.getValue().getStatus()).isEqualTo(TicketStatus.CANCELLED);
            verify(seatMapService).updateSeatStatus(
                    eq(scheduleId), eq(travelDate), eq(5), eq(1), eq(SeatStatus.AVAILABLE), eq(ticketId));
        }

        @Test
        @DisplayName("Should throw BusinessException when cancelling already cancelled ticket")
        void shouldThrowExceptionWhenCancellingAlreadyCancelledTicket() {
            Ticket cancelledTicket = buildTicket(ticketId, TicketStatus.CANCELLED);
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(cancelledTicket));

            assertThatThrownBy(() -> ticketService.cancel(ticketId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya está cancelado");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ticket not found")
        void shouldThrowExceptionWhenTicketNotFound() {
            given(ticketRepository.findById(ticketId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.cancel(ticketId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should not update seat map when ticket not found")
        void shouldNotUpdateSeatMapWhenTicketNotFound() {
            given(ticketRepository.findById(ticketId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.cancel(ticketId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(seatMapService, never()).updateSeatStatus(any(), any(), anyInt(), anyInt(), any(), any());
        }
    }

    @Nested
    @DisplayName("changeSeat() tests")
    class ChangeSeatTests {

        @Test
        @DisplayName("Should change seat when new seat is available")
        void shouldChangeSeatWhenAvailable() {
            ChangeSeatRequest request = new ChangeSeatRequest(10, 1);
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(confirmedTicket));
            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    any(), any(), eq(10), eq(1), any())).willReturn(false);
            given(ticketRepository.save(any())).willReturn(confirmedTicket);
            given(ticketMapper.toResponse(any())).willReturn(ticketResponse);

            ticketService.changeSeat(ticketId, request);

            verify(seatMapService).updateSeatStatus(
                    eq(scheduleId), eq(travelDate), eq(5), eq(1), eq(SeatStatus.AVAILABLE), eq(ticketId));
            verify(seatMapService).updateSeatStatus(
                    eq(scheduleId), eq(travelDate), eq(10), eq(1), eq(SeatStatus.SOLD), eq(ticketId));

            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(captor.capture());
            assertThat(captor.getValue().getSeatNumber()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should throw exception when new seat is taken")
        void shouldThrowExceptionWhenNewSeatIsTaken() {
            ChangeSeatRequest request = new ChangeSeatRequest(10, 1);
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(confirmedTicket));
            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    any(), any(), eq(10), eq(1), any())).willReturn(true);

            assertThatThrownBy(() -> ticketService.changeSeat(ticketId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya está ocupado");
        }

        @Test
        @DisplayName("Should throw exception when ticket is CANCELLED")
        void shouldThrowExceptionWhenTicketIsCancelled() {
            ChangeSeatRequest request = new ChangeSeatRequest(10, 1);
            Ticket cancelledTicket = buildTicket(ticketId, TicketStatus.CANCELLED);
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(cancelledTicket));

            assertThatThrownBy(() -> ticketService.changeSeat(ticketId, request))
                    .isInstanceOf(BusinessException.class);

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when ticket is RESCHEDULED")
        void shouldThrowExceptionWhenTicketIsRescheduled() {
            ChangeSeatRequest request = new ChangeSeatRequest(10, 1);
            Ticket rescheduledTicket = buildTicket(ticketId, TicketStatus.RESCHEDULED);
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(rescheduledTicket));

            assertThatThrownBy(() -> ticketService.changeSeat(ticketId, request))
                    .isInstanceOf(BusinessException.class);

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update new floor number when changing seat")
        void shouldUpdateFloorNumberWhenChangingSeat() {
            ChangeSeatRequest request = new ChangeSeatRequest(3, 2);
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(confirmedTicket));
            given(ticketRepository.existsByScheduleIdAndTravelDateAndSeatNumberAndFloorNumberAndStatusNot(
                    any(), any(), eq(3), eq(2), any())).willReturn(false);
            given(ticketRepository.save(any())).willReturn(confirmedTicket);
            given(ticketMapper.toResponse(any())).willReturn(ticketResponse);

            ticketService.changeSeat(ticketId, request);

            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(captor.capture());
            assertThat(captor.getValue().getSeatNumber()).isEqualTo(3);
            assertThat(captor.getValue().getFloorNumber()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("updateCustomerInfo() tests")
    class UpdateCustomerTests {

        @Test
        @DisplayName("Should update passenger info on confirmed ticket")
        void shouldUpdatePassengerInfo() {
            UpdateTicketCustomerRequest request = new UpdateTicketCustomerRequest(
                    customerId, "Juan Pérez", "12345678"
            );
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(confirmedTicket));
            given(ticketRepository.save(any())).willReturn(confirmedTicket);
            given(ticketMapper.toResponse(any())).willReturn(ticketResponse);

            ticketService.updateCustomerInfo(ticketId, request);

            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(captor.capture());
            assertThat(captor.getValue().getPassengerName()).isEqualTo("Juan Pérez");
            assertThat(captor.getValue().getPassengerDocument()).isEqualTo("12345678");
        }

        @Test
        @DisplayName("Should throw exception when updating cancelled ticket")
        void shouldThrowExceptionWhenUpdatingCancelledTicket() {
            Ticket cancelledTicket = buildTicket(ticketId, TicketStatus.CANCELLED);
            UpdateTicketCustomerRequest request = new UpdateTicketCustomerRequest(
                    customerId, "Juan", "123"
            );
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(cancelledTicket));

            assertThatThrownBy(() -> ticketService.updateCustomerInfo(ticketId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cancelado");
        }

        @Test
        @DisplayName("Should update only non-null fields")
        void shouldUpdateOnlyNonNullFields() {
            confirmedTicket.setPassengerName("Original Name");
            confirmedTicket.setPassengerDocument("99999999");
            UpdateTicketCustomerRequest request = new UpdateTicketCustomerRequest(
                    null, "New Name", null
            );
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(confirmedTicket));
            given(ticketRepository.save(any())).willReturn(confirmedTicket);
            given(ticketMapper.toResponse(any())).willReturn(ticketResponse);

            ticketService.updateCustomerInfo(ticketId, request);

            ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
            verify(ticketRepository).save(captor.capture());
            assertThat(captor.getValue().getPassengerName()).isEqualTo("New Name");
            // document should remain unchanged since null was passed
            assertThat(captor.getValue().getPassengerDocument()).isEqualTo("99999999");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ticket not found")
        void shouldThrowResourceNotFoundWhenTicketMissing() {
            UpdateTicketCustomerRequest request = new UpdateTicketCustomerRequest(
                    customerId, "Juan", "123"
            );
            given(ticketRepository.findById(ticketId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.updateCustomerInfo(ticketId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(ticketRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findById() tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return ticket response when found")
        void shouldReturnTicketWhenFound() {
            given(ticketRepository.findById(ticketId)).willReturn(Optional.of(confirmedTicket));
            given(ticketMapper.toResponse(confirmedTicket)).willReturn(ticketResponse);

            TicketResponse result = ticketService.findById(ticketId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(ticketId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when ticket not found")
        void shouldThrowExceptionWhenNotFound() {
            given(ticketRepository.findById(ticketId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.findById(ticketId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findByCode() tests")
    class FindByCodeTests {

        @Test
        @DisplayName("Should return ticket response when code found")
        void shouldReturnTicketWhenCodeFound() {
            given(ticketRepository.findByTicketCode("TKT-TEST001")).willReturn(Optional.of(confirmedTicket));
            given(ticketMapper.toResponse(confirmedTicket)).willReturn(ticketResponse);

            TicketResponse result = ticketService.findByCode("TKT-TEST001");

            assertThat(result).isNotNull();
            assertThat(result.ticketCode()).isEqualTo("TKT-TEST001");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when code not found")
        void shouldThrowExceptionWhenCodeNotFound() {
            given(ticketRepository.findByTicketCode("INVALID")).willReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.findByCode("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("INVALID");
        }
    }

    @Nested
    @DisplayName("findAll() tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return page of tickets excluding cancelled")
        void shouldReturnPageExcludingCancelled() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ticket> page = new PageImpl<>(List.of(confirmedTicket));
            given(ticketRepository.findAllByStatusNot(TicketStatus.CANCELLED, pageable)).willReturn(page);
            given(ticketMapper.toResponse(confirmedTicket)).willReturn(ticketResponse);

            PageResponse<TicketResponse> result = ticketService.findAll(pageable);

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
        }
    }
}
