package com.transporte.pasajes.service;

import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.pasajes.dto.RefundRequest;
import com.transporte.pasajes.dto.RefundResponse;
import com.transporte.pasajes.entity.Refund;
import com.transporte.pasajes.entity.Ticket;
import com.transporte.pasajes.enums.RefundStatus;
import com.transporte.pasajes.enums.SaleType;
import com.transporte.pasajes.enums.TicketStatus;
import com.transporte.pasajes.mapper.RefundMapper;
import com.transporte.pasajes.repository.RefundRepository;
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
@DisplayName("RefundService Unit Tests")
class RefundServiceTest {

    @Mock private RefundRepository refundRepository;
    @Mock private TicketService ticketService;
    @Mock private RefundMapper refundMapper;

    @InjectMocks
    private RefundService refundService;

    private UUID ticketId;
    private UUID refundId;
    private UUID employeeId;
    private Ticket confirmedTicket;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        refundId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        confirmedTicket = buildTicket(ticketId, TicketStatus.CONFIRMED, new BigDecimal("200.00"));
    }

    private Ticket buildTicket(UUID id, TicketStatus status, BigDecimal price) {
        Ticket ticket = new Ticket();
        try {
            var idField = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(ticket, id);
        } catch (Exception e) {
            // ignore
        }
        ticket.setTicketCode("TKT-REF001");
        ticket.setScheduleId(UUID.randomUUID());
        ticket.setCustomerId(UUID.randomUUID());
        ticket.setSeatNumber(10);
        ticket.setFloorNumber(1);
        ticket.setTravelDate(LocalDate.now().plusDays(5));
        ticket.setPrice(price);
        ticket.setStatus(status);
        ticket.setSaleType(SaleType.VENTANILLA);
        return ticket;
    }

    private Refund buildRefund(UUID id, UUID tktId, RefundStatus status,
                                BigDecimal original, BigDecimal retained, BigDecimal refunded) {
        Refund refund = Refund.builder()
                .ticketId(tktId)
                .reason("Customer request")
                .retentionPercent(retained.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : retained.multiply(BigDecimal.valueOf(100)).divide(original, 2, java.math.RoundingMode.HALF_UP))
                .originalAmount(original)
                .retainedAmount(retained)
                .refundedAmount(refunded)
                .status(status)
                .build();
        try {
            var idField = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(refund, id);
        } catch (Exception e) {
            // ignore
        }
        return refund;
    }

    private RefundResponse buildRefundResponse(UUID id, UUID tktId, RefundStatus status,
                                                BigDecimal original, BigDecimal retained, BigDecimal refunded) {
        return new RefundResponse(id, tktId, "Customer request",
                BigDecimal.ZERO, original, retained, refunded, status, LocalDateTime.now());
    }

    @Nested
    @DisplayName("requestRefund() tests")
    class RequestRefundTests {

        @Test
        @DisplayName("Should calculate amounts correctly with zero retention")
        void shouldCalculateAmountsWithZeroRetention() {
            RefundRequest request = new RefundRequest(ticketId, "Cancelled trip", BigDecimal.ZERO, employeeId);
            BigDecimal originalAmount = new BigDecimal("200.00");

            Refund savedRefund = buildRefund(UUID.randomUUID(), ticketId, RefundStatus.PENDING,
                    originalAmount, BigDecimal.ZERO, originalAmount);
            RefundResponse response = buildRefundResponse(refundId, ticketId, RefundStatus.PENDING,
                    originalAmount, BigDecimal.ZERO, originalAmount);

            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(refundRepository.existsByTicketId(ticketId)).willReturn(false);
            given(refundRepository.save(any())).willReturn(savedRefund);
            given(refundMapper.toResponse(savedRefund)).willReturn(response);

            RefundResponse result = refundService.requestRefund(request);

            assertThat(result).isNotNull();

            ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
            verify(refundRepository).save(captor.capture());
            Refund capturedRefund = captor.getValue();

            assertThat(capturedRefund.getOriginalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(capturedRefund.getRetainedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(capturedRefund.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(capturedRefund.getStatus()).isEqualTo(RefundStatus.PENDING);
        }

        @Test
        @DisplayName("Should calculate amounts correctly with 10% retention")
        void shouldCalculateAmountsWithTenPercentRetention() {
            BigDecimal retentionPercent = new BigDecimal("10");
            BigDecimal originalAmount = new BigDecimal("200.00");
            BigDecimal expectedRetained = new BigDecimal("20.00");
            BigDecimal expectedRefunded = new BigDecimal("180.00");

            RefundRequest request = new RefundRequest(ticketId, "Late cancellation", retentionPercent, employeeId);

            Refund savedRefund = buildRefund(UUID.randomUUID(), ticketId, RefundStatus.PENDING,
                    originalAmount, expectedRetained, expectedRefunded);
            RefundResponse response = buildRefundResponse(refundId, ticketId, RefundStatus.PENDING,
                    originalAmount, expectedRetained, expectedRefunded);

            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(refundRepository.existsByTicketId(ticketId)).willReturn(false);
            given(refundRepository.save(any())).willReturn(savedRefund);
            given(refundMapper.toResponse(savedRefund)).willReturn(response);

            refundService.requestRefund(request);

            ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
            verify(refundRepository).save(captor.capture());
            Refund capturedRefund = captor.getValue();

            assertThat(capturedRefund.getOriginalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(capturedRefund.getRetainedAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(capturedRefund.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("180.00"));
        }

        @Test
        @DisplayName("Should calculate amounts correctly with 25% retention")
        void shouldCalculateAmountsWithTwentyFivePercentRetention() {
            BigDecimal retentionPercent = new BigDecimal("25");
            BigDecimal originalAmount = new BigDecimal("150.00");
            // 25% of 150 = 37.50 retained; 112.50 refunded
            BigDecimal expectedRetained = new BigDecimal("37.50");
            BigDecimal expectedRefunded = new BigDecimal("112.50");

            Ticket ticketWith150 = buildTicket(ticketId, TicketStatus.CONFIRMED, new BigDecimal("150.00"));
            RefundRequest request = new RefundRequest(ticketId, "Penalty fee", retentionPercent, employeeId);

            Refund savedRefund = buildRefund(UUID.randomUUID(), ticketId, RefundStatus.PENDING,
                    originalAmount, expectedRetained, expectedRefunded);
            RefundResponse response = buildRefundResponse(refundId, ticketId, RefundStatus.PENDING,
                    originalAmount, expectedRetained, expectedRefunded);

            given(ticketService.findTicketById(ticketId)).willReturn(ticketWith150);
            given(refundRepository.existsByTicketId(ticketId)).willReturn(false);
            given(refundRepository.save(any())).willReturn(savedRefund);
            given(refundMapper.toResponse(savedRefund)).willReturn(response);

            refundService.requestRefund(request);

            ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
            verify(refundRepository).save(captor.capture());
            Refund capturedRefund = captor.getValue();

            assertThat(capturedRefund.getRetainedAmount()).isEqualByComparingTo(new BigDecimal("37.50"));
            assertThat(capturedRefund.getRefundedAmount()).isEqualByComparingTo(new BigDecimal("112.50"));
        }

        @Test
        @DisplayName("Should use zero retention when retentionPercent is null")
        void shouldUseZeroRetentionWhenNull() {
            RefundRequest request = new RefundRequest(ticketId, "Cancelled trip", null, employeeId);
            BigDecimal originalAmount = new BigDecimal("200.00");

            Refund savedRefund = buildRefund(UUID.randomUUID(), ticketId, RefundStatus.PENDING,
                    originalAmount, BigDecimal.ZERO, originalAmount);
            RefundResponse response = buildRefundResponse(refundId, ticketId, RefundStatus.PENDING,
                    originalAmount, BigDecimal.ZERO, originalAmount);

            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(refundRepository.existsByTicketId(ticketId)).willReturn(false);
            given(refundRepository.save(any())).willReturn(savedRefund);
            given(refundMapper.toResponse(savedRefund)).willReturn(response);

            refundService.requestRefund(request);

            ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
            verify(refundRepository).save(captor.capture());
            assertThat(captor.getValue().getRetainedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(captor.getValue().getRefundedAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @Test
        @DisplayName("Should throw BusinessException when ticket is not CONFIRMED")
        void shouldThrowExceptionWhenTicketIsNotConfirmed() {
            Ticket cancelledTicket = buildTicket(ticketId, TicketStatus.CANCELLED, new BigDecimal("200.00"));
            RefundRequest request = new RefundRequest(ticketId, "Reason", BigDecimal.ZERO, employeeId);

            given(ticketService.findTicketById(ticketId)).willReturn(cancelledTicket);

            assertThatThrownBy(() -> refundService.requestRefund(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("confirmados");

            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when ticket is PENDING status")
        void shouldThrowExceptionWhenTicketIsPending() {
            Ticket pendingTicket = buildTicket(ticketId, TicketStatus.PENDING, new BigDecimal("200.00"));
            RefundRequest request = new RefundRequest(ticketId, "Reason", BigDecimal.ZERO, employeeId);

            given(ticketService.findTicketById(ticketId)).willReturn(pendingTicket);

            assertThatThrownBy(() -> refundService.requestRefund(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("confirmados");
        }

        @Test
        @DisplayName("Should throw BusinessException when refund already exists for ticket")
        void shouldThrowExceptionWhenRefundAlreadyExists() {
            RefundRequest request = new RefundRequest(ticketId, "Duplicate request", BigDecimal.ZERO, employeeId);

            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(refundRepository.existsByTicketId(ticketId)).willReturn(true);

            assertThatThrownBy(() -> refundService.requestRefund(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ya existe");

            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set refund status to PENDING on creation")
        void shouldSetStatusToPendingOnCreation() {
            RefundRequest request = new RefundRequest(ticketId, "Reason", BigDecimal.ZERO, employeeId);
            BigDecimal originalAmount = new BigDecimal("200.00");

            Refund savedRefund = buildRefund(UUID.randomUUID(), ticketId, RefundStatus.PENDING,
                    originalAmount, BigDecimal.ZERO, originalAmount);
            RefundResponse response = buildRefundResponse(refundId, ticketId, RefundStatus.PENDING,
                    originalAmount, BigDecimal.ZERO, originalAmount);

            given(ticketService.findTicketById(ticketId)).willReturn(confirmedTicket);
            given(refundRepository.existsByTicketId(ticketId)).willReturn(false);
            given(refundRepository.save(any())).willReturn(savedRefund);
            given(refundMapper.toResponse(savedRefund)).willReturn(response);

            refundService.requestRefund(request);

            ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
            verify(refundRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(RefundStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("approve() tests")
    class ApproveTests {

        @Test
        @DisplayName("Should approve refund and cancel ticket")
        void shouldApproveRefundAndCancelTicket() {
            BigDecimal originalAmount = new BigDecimal("200.00");
            Refund pendingRefund = buildRefund(refundId, ticketId, RefundStatus.PENDING,
                    originalAmount, BigDecimal.ZERO, originalAmount);
            RefundResponse approvedResponse = buildRefundResponse(refundId, ticketId, RefundStatus.APPROVED,
                    originalAmount, BigDecimal.ZERO, originalAmount);

            given(refundRepository.findById(refundId)).willReturn(Optional.of(pendingRefund));
            given(refundRepository.save(any())).willReturn(pendingRefund);
            given(refundMapper.toResponse(any())).willReturn(approvedResponse);

            RefundResponse result = refundService.approve(refundId);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(RefundStatus.APPROVED);

            ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
            verify(refundRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(RefundStatus.APPROVED);

            // Verify ticket is cancelled
            verify(ticketService).cancel(ticketId);
        }

        @Test
        @DisplayName("Should throw BusinessException when refund is not PENDING")
        void shouldThrowExceptionWhenRefundIsNotPending() {
            BigDecimal originalAmount = new BigDecimal("200.00");
            Refund approvedRefund = buildRefund(refundId, ticketId, RefundStatus.APPROVED,
                    originalAmount, BigDecimal.ZERO, originalAmount);

            given(refundRepository.findById(refundId)).willReturn(Optional.of(approvedRefund));

            assertThatThrownBy(() -> refundService.approve(refundId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PENDIENTE");

            verify(ticketService, never()).cancel(any());
            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when approving already rejected refund")
        void shouldThrowExceptionWhenApprovingRejectedRefund() {
            BigDecimal originalAmount = new BigDecimal("200.00");
            Refund rejectedRefund = buildRefund(refundId, ticketId, RefundStatus.REJECTED,
                    originalAmount, BigDecimal.ZERO, originalAmount);

            given(refundRepository.findById(refundId)).willReturn(Optional.of(rejectedRefund));

            assertThatThrownBy(() -> refundService.approve(refundId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PENDIENTE");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when refund not found")
        void shouldThrowExceptionWhenRefundNotFound() {
            given(refundRepository.findById(refundId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> refundService.approve(refundId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(ticketService, never()).cancel(any());
        }
    }

    @Nested
    @DisplayName("reject() tests")
    class RejectTests {

        @Test
        @DisplayName("Should reject refund and update reason")
        void shouldRejectRefundAndUpdateReason() {
            BigDecimal originalAmount = new BigDecimal("200.00");
            Refund pendingRefund = buildRefund(refundId, ticketId, RefundStatus.PENDING,
                    originalAmount, BigDecimal.ZERO, originalAmount);
            RefundResponse rejectedResponse = buildRefundResponse(refundId, ticketId, RefundStatus.REJECTED,
                    originalAmount, BigDecimal.ZERO, originalAmount);

            given(refundRepository.findById(refundId)).willReturn(Optional.of(pendingRefund));
            given(refundRepository.save(any())).willReturn(pendingRefund);
            given(refundMapper.toResponse(any())).willReturn(rejectedResponse);

            RefundResponse result = refundService.reject(refundId, "Policy not met");

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(RefundStatus.REJECTED);

            ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
            verify(refundRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(RefundStatus.REJECTED);
            assertThat(captor.getValue().getReason()).isEqualTo("Policy not met");
        }

        @Test
        @DisplayName("Should throw BusinessException when refund is not PENDING")
        void shouldThrowExceptionWhenRefundIsNotPending() {
            BigDecimal originalAmount = new BigDecimal("200.00");
            Refund approvedRefund = buildRefund(refundId, ticketId, RefundStatus.APPROVED,
                    originalAmount, BigDecimal.ZERO, originalAmount);

            given(refundRepository.findById(refundId)).willReturn(Optional.of(approvedRefund));

            assertThatThrownBy(() -> refundService.reject(refundId, "Too late"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PENDIENTE");

            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when rejecting already rejected refund")
        void shouldThrowExceptionWhenRejectingAlreadyRejectedRefund() {
            BigDecimal originalAmount = new BigDecimal("200.00");
            Refund rejectedRefund = buildRefund(refundId, ticketId, RefundStatus.REJECTED,
                    originalAmount, BigDecimal.ZERO, originalAmount);

            given(refundRepository.findById(refundId)).willReturn(Optional.of(rejectedRefund));

            assertThatThrownBy(() -> refundService.reject(refundId, "Duplicate rejection"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("PENDIENTE");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when refund not found")
        void shouldThrowExceptionWhenRefundNotFound() {
            given(refundRepository.findById(refundId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> refundService.reject(refundId, "Not found rejection"))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not cancel ticket when rejecting refund")
        void shouldNotCancelTicketWhenRejectingRefund() {
            BigDecimal originalAmount = new BigDecimal("200.00");
            Refund pendingRefund = buildRefund(refundId, ticketId, RefundStatus.PENDING,
                    originalAmount, BigDecimal.ZERO, originalAmount);
            RefundResponse rejectedResponse = buildRefundResponse(refundId, ticketId, RefundStatus.REJECTED,
                    originalAmount, BigDecimal.ZERO, originalAmount);

            given(refundRepository.findById(refundId)).willReturn(Optional.of(pendingRefund));
            given(refundRepository.save(any())).willReturn(pendingRefund);
            given(refundMapper.toResponse(any())).willReturn(rejectedResponse);

            refundService.reject(refundId, "Policy violation");

            verify(ticketService, never()).cancel(any());
        }
    }
}
