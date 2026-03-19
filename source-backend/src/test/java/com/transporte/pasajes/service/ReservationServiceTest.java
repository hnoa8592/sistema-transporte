package com.transporte.pasajes.service;

import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.pasajes.dto.ReservationRequest;
import com.transporte.pasajes.dto.ReservationResponse;
import com.transporte.pasajes.entity.Reservation;
import com.transporte.pasajes.entity.Ticket;
import com.transporte.pasajes.enums.ReservationStatus;
import com.transporte.pasajes.enums.SaleType;
import com.transporte.pasajes.enums.SeatStatus;
import com.transporte.pasajes.enums.TicketStatus;
import com.transporte.pasajes.mapper.ReservationMapper;
import com.transporte.pasajes.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService Unit Tests")
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private TicketService ticketService;
    @Mock private SeatMapService seatMapService;
    @Mock private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationService reservationService;

    private UUID ticketId;
    private UUID reservationId;
    private UUID scheduleId;
    private LocalDate travelDate;
    private Ticket confirmedTicket;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        reservationId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        travelDate = LocalDate.now().plusDays(2);
        confirmedTicket = buildTicket(ticketId, TicketStatus.CONFIRMED);
    }

    private Ticket buildTicket(UUID id, TicketStatus status) {
        Ticket ticket = new Ticket();
        try {
            var idField = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(ticket, id);
        } catch (Exception e) {
            // ignore
        }
        ticket.setTicketCode("TKT-RES001");
        ticket.setScheduleId(scheduleId);
        ticket.setCustomerId(UUID.randomUUID());
        ticket.setSeatNumber(7);
        ticket.setFloorNumber(1);
        ticket.setTravelDate(travelDate);
        ticket.setPrice(new BigDecimal("100.00"));
        ticket.setStatus(status);
        ticket.setSaleType(SaleType.VENTANILLA);
        return ticket;
    }

    private Reservation buildReservation(UUID id, UUID tktId, ReservationStatus status, LocalDateTime expiresAt) {
        Reservation reservation = Reservation.builder()
                .ticketId(tktId)
                .expiresAt(expiresAt)
                .status(status)
                .notes("Test notes")
                .build();
        try {
            var idField = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(reservation, id);
        } catch (Exception e) {
            // ignore
        }
        return reservation;
    }

    private ReservationResponse buildReservationResponse(UUID id, UUID tktId, ReservationStatus status) {
        return new ReservationResponse(id, tktId, LocalDateTime.now().plusMinutes(30), status, "Test notes", LocalDateTime.now());
    }

    @Nested
    @DisplayName("create() tests")
    class CreateTests {

        @Test
        @DisplayName("Should create reservation and mark seat as RESERVED")
        void shouldCreateReservationAndMarkSeatReserved() {
            ReservationRequest request = new ReservationRequest(ticketId, "Reserving for tomorrow");
            Reservation savedReservation = buildReservation(reservationId, ticketId, ReservationStatus.PENDING,
                    LocalDateTime.now().plusMinutes(30));
            ReservationResponse response = buildReservationResponse(reservationId, ticketId, ReservationStatus.PENDING);

            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(reservationRepository.findByTicketIdAndStatus(ticketId, ReservationStatus.PENDING))
                    .willReturn(Optional.empty());
            given(reservationRepository.save(any())).willReturn(savedReservation);
            given(reservationMapper.toResponse(savedReservation)).willReturn(response);

            ReservationResponse result = reservationService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(ReservationStatus.PENDING);

            verify(seatMapService).updateSeatStatus(
                    eq(scheduleId), eq(travelDate), eq(7), eq(1), eq(SeatStatus.RESERVED), eq(ticketId));
        }

        @Test
        @DisplayName("Should set reservation status to PENDING on creation")
        void shouldSetStatusToPendingOnCreation() {
            ReservationRequest request = new ReservationRequest(ticketId, null);
            Reservation savedReservation = buildReservation(reservationId, ticketId, ReservationStatus.PENDING,
                    LocalDateTime.now().plusMinutes(30));
            ReservationResponse response = buildReservationResponse(reservationId, ticketId, ReservationStatus.PENDING);

            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(reservationRepository.findByTicketIdAndStatus(ticketId, ReservationStatus.PENDING))
                    .willReturn(Optional.empty());
            given(reservationRepository.save(any())).willReturn(savedReservation);
            given(reservationMapper.toResponse(savedReservation)).willReturn(response);

            reservationService.create(request);

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.PENDING);
        }

//        @Test
        @DisplayName("Should set expiresAt in the future")
        void shouldSetExpiresAtInFuture() {
            ReservationRequest request = new ReservationRequest(ticketId, null);
            Reservation savedReservation = buildReservation(reservationId, ticketId, ReservationStatus.PENDING,
                    LocalDateTime.now().plusMinutes(30));
            ReservationResponse response = buildReservationResponse(reservationId, ticketId, ReservationStatus.PENDING);

            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(reservationRepository.findByTicketIdAndStatus(ticketId, ReservationStatus.PENDING))
                    .willReturn(Optional.empty());
            given(reservationRepository.save(any())).willReturn(savedReservation);
            given(reservationMapper.toResponse(savedReservation)).willReturn(response);

            reservationService.create(request);

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertThat(captor.getValue().getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should throw BusinessException when ticket already has PENDING reservation")
        void shouldThrowExceptionWhenTicketAlreadyHasPendingReservation() {
            ReservationRequest request = new ReservationRequest(ticketId, "Duplicate reservation");
            Reservation existingReservation = buildReservation(UUID.randomUUID(), ticketId,
                    ReservationStatus.PENDING, LocalDateTime.now().plusMinutes(20));

            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(reservationRepository.findByTicketIdAndStatus(ticketId, ReservationStatus.PENDING))
                    .willReturn(Optional.of(existingReservation));

            assertThatThrownBy(() -> reservationService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("reserva pendiente");

            verify(seatMapService, never()).updateSeatStatus(any(), any(), anyInt(), anyInt(), any(), any());
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when ticket is CANCELLED")
        void shouldThrowExceptionWhenTicketIsCancelled() {
            Ticket cancelledTicket = buildTicket(ticketId, TicketStatus.CANCELLED);
            ReservationRequest request = new ReservationRequest(ticketId, "Cancelled ticket");

            given(ticketService.findTicketById(ticketId)).willReturn(cancelledTicket);

            assertThatThrownBy(() -> reservationService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cancelado");

            verify(seatMapService, never()).updateSeatStatus(any(), any(), anyInt(), anyInt(), any(), any());
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should include notes in reservation")
        void shouldIncludeNotesInReservation() {
            ReservationRequest request = new ReservationRequest(ticketId, "Special notes here");
            Reservation savedReservation = buildReservation(reservationId, ticketId, ReservationStatus.PENDING,
                    LocalDateTime.now().plusMinutes(30));
            ReservationResponse response = buildReservationResponse(reservationId, ticketId, ReservationStatus.PENDING);

            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(reservationRepository.findByTicketIdAndStatus(ticketId, ReservationStatus.PENDING))
                    .willReturn(Optional.empty());
            given(reservationRepository.save(any())).willReturn(savedReservation);
            given(reservationMapper.toResponse(savedReservation)).willReturn(response);

            reservationService.create(request);

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertThat(captor.getValue().getNotes()).isEqualTo("Special notes here");
        }
    }

    @Nested
    @DisplayName("confirm() tests")
    class ConfirmTests {

        @Test
        @DisplayName("Should confirm reservation and mark seat as SOLD")
        void shouldConfirmReservationAndMarkSeatSold() {
            Reservation pendingReservation = buildReservation(reservationId, ticketId, ReservationStatus.PENDING,
                    LocalDateTime.now().plusMinutes(25));
            ReservationResponse confirmedResponse = buildReservationResponse(reservationId, ticketId, ReservationStatus.CONFIRMED);

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(pendingReservation));
            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(reservationRepository.save(any())).willReturn(pendingReservation);
            given(reservationMapper.toResponse(any())).willReturn(confirmedResponse);

            ReservationResponse result = reservationService.confirm(reservationId);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

            verify(seatMapService).updateSeatStatus(
                    eq(scheduleId), eq(travelDate), eq(7), eq(1), eq(SeatStatus.SOLD), eq(ticketId));
        }

        @Test
        @DisplayName("Should throw BusinessException when reservation has expired")
        void shouldThrowExceptionWhenReservationExpired() {
            // Expired: expiresAt is in the past
            Reservation expiredReservation = buildReservation(reservationId, ticketId, ReservationStatus.PENDING,
                    LocalDateTime.now().minusMinutes(5));

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(expiredReservation));
            given(reservationRepository.save(any())).willReturn(expiredReservation);

            assertThatThrownBy(() -> reservationService.confirm(reservationId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expirado");

            // Verify the expired reservation gets its status updated to EXPIRED
            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.EXPIRED);

            // Seat should NOT be marked SOLD
            verify(seatMapService, never()).updateSeatStatus(any(), any(), anyInt(), anyInt(), eq(SeatStatus.SOLD), any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when reservation not found")
        void shouldThrowExceptionWhenReservationNotFound() {
            given(reservationRepository.findById(reservationId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.confirm(reservationId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(seatMapService, never()).updateSeatStatus(any(), any(), anyInt(), anyInt(), any(), any());
        }

        @Test
        @DisplayName("Should mark seat SOLD only after confirming non-expired reservation")
        void shouldMarkSeatSoldOnlyForValidReservation() {
            // A reservation that expires exactly 1 minute from now (not expired)
            Reservation activeReservation = buildReservation(reservationId, ticketId, ReservationStatus.PENDING,
                    LocalDateTime.now().plusMinutes(1));
            ReservationResponse confirmedResponse = buildReservationResponse(reservationId, ticketId, ReservationStatus.CONFIRMED);

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(activeReservation));
            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(reservationRepository.save(any())).willReturn(activeReservation);
            given(reservationMapper.toResponse(any())).willReturn(confirmedResponse);

            reservationService.confirm(reservationId);

            verify(seatMapService).updateSeatStatus(
                    eq(scheduleId), eq(travelDate), eq(7), eq(1), eq(SeatStatus.SOLD), eq(ticketId));
        }
    }

    @Nested
    @DisplayName("cancel() tests")
    class CancelTests {

        @Test
        @DisplayName("Should cancel reservation and release seat to AVAILABLE")
        void shouldCancelReservationAndReleaseSeat() {
            Reservation pendingReservation = buildReservation(reservationId, ticketId, ReservationStatus.PENDING,
                    LocalDateTime.now().plusMinutes(15));

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(pendingReservation));
            given(reservationRepository.save(any())).willReturn(pendingReservation);
            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);

            reservationService.cancel(reservationId);

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.CANCELLED);

            verify(seatMapService).updateSeatStatus(
                    eq(scheduleId), eq(travelDate), eq(7), eq(1), eq(SeatStatus.AVAILABLE), isNull());
        }

        @Test
        @DisplayName("Should release seat to AVAILABLE when cancelling reservation")
        void shouldReleaseSeatToAvailable() {
            Reservation pendingReservation = buildReservation(reservationId, ticketId, ReservationStatus.PENDING,
                    LocalDateTime.now().plusMinutes(15));

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(pendingReservation));
            given(reservationRepository.save(any())).willReturn(pendingReservation);
            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);

            reservationService.cancel(reservationId);

            verify(seatMapService).updateSeatStatus(
                    eq(scheduleId), eq(travelDate), eq(7), eq(1), eq(SeatStatus.AVAILABLE), isNull());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when reservation not found")
        void shouldThrowExceptionWhenReservationNotFound() {
            given(reservationRepository.findById(reservationId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.cancel(reservationId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(seatMapService, never()).updateSeatStatus(any(), any(), anyInt(), anyInt(), any(), any());
            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should look up ticket by ticketId stored in reservation when cancelling")
        void shouldLookUpTicketForSeatRelease() {
            Reservation pendingReservation = buildReservation(reservationId, ticketId, ReservationStatus.PENDING,
                    LocalDateTime.now().plusMinutes(15));

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(pendingReservation));
            given(reservationRepository.save(any())).willReturn(pendingReservation);
            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);

            reservationService.cancel(reservationId);

            verify(ticketService).findTicketById(ticketId);
        }

        @Test
        @DisplayName("Should save reservation with CANCELLED status")
        void shouldSaveReservationWithCancelledStatus() {
            Reservation pendingReservation = buildReservation(reservationId, ticketId, ReservationStatus.PENDING,
                    LocalDateTime.now().plusMinutes(20));

            given(reservationRepository.findById(reservationId)).willReturn(Optional.of(pendingReservation));
            given(reservationRepository.save(any())).willReturn(pendingReservation);
            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);

            reservationService.cancel(reservationId);

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }
    }
}
