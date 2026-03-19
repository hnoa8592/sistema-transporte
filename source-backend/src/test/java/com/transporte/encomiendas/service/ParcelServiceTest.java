package com.transporte.encomiendas.service;

import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.encomiendas.dto.ParcelRequest;
import com.transporte.encomiendas.dto.ParcelResponse;
import com.transporte.encomiendas.dto.ParcelTrackingResponse;
import com.transporte.encomiendas.dto.UpdateParcelStatusRequest;
import com.transporte.encomiendas.entity.Parcel;
import com.transporte.encomiendas.entity.ParcelTracking;
import com.transporte.encomiendas.enums.ParcelStatus;
import com.transporte.encomiendas.mapper.ParcelMapper;
import com.transporte.encomiendas.mapper.ParcelTrackingMapper;
import com.transporte.encomiendas.repository.ParcelRepository;
import com.transporte.encomiendas.repository.ParcelTrackingRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParcelService Unit Tests")
class ParcelServiceTest {

    @Mock private ParcelRepository parcelRepository;
    @Mock private ParcelTrackingRepository trackingRepository;
    @Mock private ParcelMapper parcelMapper;
    @Mock private ParcelTrackingMapper trackingMapper;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ParcelService parcelService;

    private UUID parcelId;
    private Parcel testParcel;
    private ParcelResponse testParcelResponse;

    @BeforeEach
    void setUp() {
        parcelId = UUID.randomUUID();
        testParcel = buildParcel(parcelId, ParcelStatus.RECIBIDO);
        testParcelResponse = buildParcelResponse(parcelId, ParcelStatus.RECIBIDO);
    }

    private Parcel buildParcel(UUID id, ParcelStatus status) {
        Parcel parcel = new Parcel();
        try {
            var idField = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(parcel, id);
        } catch (Exception e) {}
        parcel.setTrackingCode("PCL-20240101120000-1234");
        parcel.setSenderName("Juan Pérez");
        parcel.setRecipientName("María García");
        parcel.setWeight(new BigDecimal("2.5"));
        parcel.setPrice(new BigDecimal("50.00"));
        parcel.setStatus(status);
        return parcel;
    }

    private ParcelResponse buildParcelResponse(UUID id, ParcelStatus status) {
        return new ParcelResponse(id, "PCL-20240101120000-1234", null, "Juan Pérez", null,
                null, "María García", null, null, null,
                new BigDecimal("2.5"), null, new BigDecimal("50.00"), status, null, null);
    }

    @Nested
    @DisplayName("create() tests")
    class CreateTests {

        @Test
        @DisplayName("Should create parcel with tracking code and initial tracking record")
        void shouldCreateParcelWithTrackingCode() {
            ParcelRequest request = new ParcelRequest(
                    null, "Juan Pérez", "123456789",
                    null, "María García", "987654321",
                    null, "Ropa", new BigDecimal("2.5"),
                    new BigDecimal("500.00"), new BigDecimal("50.00"), null
            );

            given(parcelMapper.toEntity(request)).willReturn(testParcel);
            given(parcelRepository.save(any())).willReturn(testParcel);
            given(trackingRepository.save(any())).willReturn(new ParcelTracking());
            given(parcelMapper.toResponse(testParcel)).willReturn(testParcelResponse);

            ParcelResponse result = parcelService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.trackingCode()).isNotNull();

            // Verify tracking record was created
            ArgumentCaptor<ParcelTracking> trackingCaptor = ArgumentCaptor.forClass(ParcelTracking.class);
            verify(trackingRepository).save(trackingCaptor.capture());
            assertThat(trackingCaptor.getValue().getStatus()).isEqualTo(ParcelStatus.RECIBIDO);
        }

        @Test
        @DisplayName("Should set status to RECIBIDO on creation")
        void shouldSetStatusToRecibido() {
            ParcelRequest request = new ParcelRequest(
                    null, "Juan", null, null, "María", null,
                    null, null, new BigDecimal("1.0"), null, new BigDecimal("30.00"), null
            );

            given(parcelMapper.toEntity(request)).willReturn(testParcel);
            given(parcelRepository.save(any())).willReturn(testParcel);
            given(trackingRepository.save(any())).willReturn(new ParcelTracking());
            given(parcelMapper.toResponse(any())).willReturn(testParcelResponse);

            parcelService.create(request);

            ArgumentCaptor<Parcel> captor = ArgumentCaptor.forClass(Parcel.class);
            verify(parcelRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ParcelStatus.RECIBIDO);
        }

        @Test
        @DisplayName("Should generate non-null tracking code starting with PCL-")
        void shouldGenerateTrackingCodeWithPclPrefix() {
            ParcelRequest request = new ParcelRequest(
                    null, "Sender", null, null, "Recipient", null,
                    null, "Items", new BigDecimal("1.0"), null, new BigDecimal("25.00"), null
            );

            given(parcelMapper.toEntity(request)).willReturn(testParcel);
            given(parcelRepository.save(any())).willReturn(testParcel);
            given(trackingRepository.save(any())).willReturn(new ParcelTracking());
            given(parcelMapper.toResponse(any())).willReturn(testParcelResponse);

            parcelService.create(request);

            ArgumentCaptor<Parcel> captor = ArgumentCaptor.forClass(Parcel.class);
            verify(parcelRepository).save(captor.capture());
            assertThat(captor.getValue().getTrackingCode()).isNotNull();
            assertThat(captor.getValue().getTrackingCode()).startsWith("PCL-");
        }

        @Test
        @DisplayName("Should save initial tracking with RECIBIDO status and location")
        void shouldSaveInitialTrackingWithRecibidoStatus() {
            ParcelRequest request = new ParcelRequest(
                    null, "Sender", null, null, "Recipient", null,
                    null, null, new BigDecimal("1.0"), null, new BigDecimal("25.00"), null
            );

            given(parcelMapper.toEntity(request)).willReturn(testParcel);
            given(parcelRepository.save(any())).willReturn(testParcel);
            given(trackingRepository.save(any())).willReturn(new ParcelTracking());
            given(parcelMapper.toResponse(any())).willReturn(testParcelResponse);

            parcelService.create(request);

            ArgumentCaptor<ParcelTracking> trackingCaptor = ArgumentCaptor.forClass(ParcelTracking.class);
            verify(trackingRepository).save(trackingCaptor.capture());
            ParcelTracking capturedTracking = trackingCaptor.getValue();
            assertThat(capturedTracking.getStatus()).isEqualTo(ParcelStatus.RECIBIDO);
            assertThat(capturedTracking.getParcelId()).isEqualTo(parcelId);
        }
    }

    @Nested
    @DisplayName("updateStatus() tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update from RECIBIDO to EN_TRANSITO")
        void shouldUpdateFromRecibidoToEnTransito() {
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.EN_TRANSITO, "Terminal Cochabamba", "En ruta"
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(testParcel));
            given(parcelRepository.save(any())).willReturn(testParcel);
            given(trackingRepository.save(any())).willReturn(new ParcelTracking());
            given(parcelMapper.toResponse(any())).willReturn(testParcelResponse);

            parcelService.updateStatus(parcelId, request);

            ArgumentCaptor<Parcel> captor = ArgumentCaptor.forClass(Parcel.class);
            verify(parcelRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ParcelStatus.EN_TRANSITO);

            verify(trackingRepository).save(any(ParcelTracking.class));
        }

        @Test
        @DisplayName("Should update from EN_TRANSITO to EN_DESTINO")
        void shouldUpdateFromEnTransitoToEnDestino() {
            Parcel enTransitoParcel = buildParcel(parcelId, ParcelStatus.EN_TRANSITO);
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.EN_DESTINO, "Terminal Cochabamba", "Llegó a destino"
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(enTransitoParcel));
            given(parcelRepository.save(any())).willReturn(enTransitoParcel);
            given(trackingRepository.save(any())).willReturn(new ParcelTracking());
            given(parcelMapper.toResponse(any())).willReturn(testParcelResponse);

            parcelService.updateStatus(parcelId, request);

            verify(parcelRepository).save(argThat(p -> p.getStatus() == ParcelStatus.EN_DESTINO));
        }

        @Test
        @DisplayName("Should update from EN_DESTINO to ENTREGADO")
        void shouldUpdateFromEnDestinoToEntregado() {
            Parcel enDestinoParcel = buildParcel(parcelId, ParcelStatus.EN_DESTINO);
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.ENTREGADO, "Domicilio", "Entregado al destinatario"
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(enDestinoParcel));
            given(parcelRepository.save(any())).willReturn(enDestinoParcel);
            given(trackingRepository.save(any())).willReturn(new ParcelTracking());
            given(parcelMapper.toResponse(any())).willReturn(testParcelResponse);

            parcelService.updateStatus(parcelId, request);

            verify(parcelRepository).save(argThat(p -> p.getStatus() == ParcelStatus.ENTREGADO));
        }

        @Test
        @DisplayName("Should throw BusinessException for invalid status transition RECIBIDO -> EN_DESTINO")
        void shouldThrowExceptionForInvalidTransition() {
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.EN_DESTINO, "Destino", "salto ilegal"
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(testParcel));

            assertThatThrownBy(() -> parcelService.updateStatus(parcelId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Transición de estado inválida");
        }

        @Test
        @DisplayName("Should throw BusinessException when trying to update ENTREGADO parcel")
        void shouldThrowExceptionWhenParcelAlreadyDelivered() {
            Parcel deliveredParcel = buildParcel(parcelId, ParcelStatus.ENTREGADO);
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.EN_TRANSITO, null, null
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(deliveredParcel));

            assertThatThrownBy(() -> parcelService.updateStatus(parcelId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Transición de estado inválida");
        }

        @Test
        @DisplayName("Should throw BusinessException for invalid transition RECIBIDO -> ENTREGADO")
        void shouldThrowExceptionForRecibidoToEntregado() {
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.ENTREGADO, null, null
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(testParcel));

            assertThatThrownBy(() -> parcelService.updateStatus(parcelId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Transición de estado inválida");
        }

        @Test
        @DisplayName("Should throw BusinessException for invalid transition RECIBIDO -> DEVUELTO")
        void shouldThrowExceptionForRecibidoToDevuelto() {
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.DEVUELTO, null, "intento inválido"
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(testParcel));

            assertThatThrownBy(() -> parcelService.updateStatus(parcelId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Transición de estado inválida");
        }

        @Test
        @DisplayName("Should allow EN_TRANSITO -> DEVUELTO transition")
        void shouldAllowEnTransitoToDevueltoTransition() {
            Parcel enTransitoParcel = buildParcel(parcelId, ParcelStatus.EN_TRANSITO);
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.DEVUELTO, "Terminal Origen", "Devuelto por destinatario ausente"
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(enTransitoParcel));
            given(parcelRepository.save(any())).willReturn(enTransitoParcel);
            given(trackingRepository.save(any())).willReturn(new ParcelTracking());
            given(parcelMapper.toResponse(any())).willReturn(testParcelResponse);

            parcelService.updateStatus(parcelId, request);

            verify(parcelRepository).save(argThat(p -> p.getStatus() == ParcelStatus.DEVUELTO));
        }

        @Test
        @DisplayName("Should allow EN_DESTINO -> DEVUELTO transition")
        void shouldAllowEnDestinoToDevueltoTransition() {
            Parcel enDestinoParcel = buildParcel(parcelId, ParcelStatus.EN_DESTINO);
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.DEVUELTO, "Terminal Destino", "Rechazado en destino"
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(enDestinoParcel));
            given(parcelRepository.save(any())).willReturn(enDestinoParcel);
            given(trackingRepository.save(any())).willReturn(new ParcelTracking());
            given(parcelMapper.toResponse(any())).willReturn(testParcelResponse);

            parcelService.updateStatus(parcelId, request);

            verify(parcelRepository).save(argThat(p -> p.getStatus() == ParcelStatus.DEVUELTO));
        }

        @Test
        @DisplayName("Should throw BusinessException for any update when status is DEVUELTO")
        void shouldThrowExceptionWhenParcelIsDevuelto() {
            Parcel devueltoParcel = buildParcel(parcelId, ParcelStatus.DEVUELTO);
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.EN_TRANSITO, null, null
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(devueltoParcel));

            assertThatThrownBy(() -> parcelService.updateStatus(parcelId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Transición de estado inválida");
        }

        @Test
        @DisplayName("Should create tracking record when updating status")
        void shouldCreateTrackingRecordOnStatusUpdate() {
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.EN_TRANSITO, "La Paz", "Saliendo"
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(testParcel));
            given(parcelRepository.save(any())).willReturn(testParcel);
            given(trackingRepository.save(any())).willReturn(new ParcelTracking());
            given(parcelMapper.toResponse(any())).willReturn(testParcelResponse);

            parcelService.updateStatus(parcelId, request);

            ArgumentCaptor<ParcelTracking> trackingCaptor = ArgumentCaptor.forClass(ParcelTracking.class);
            verify(trackingRepository).save(trackingCaptor.capture());
            ParcelTracking capturedTracking = trackingCaptor.getValue();
            assertThat(capturedTracking.getStatus()).isEqualTo(ParcelStatus.EN_TRANSITO);
            assertThat(capturedTracking.getLocation()).isEqualTo("La Paz");
            assertThat(capturedTracking.getNotes()).isEqualTo("Saliendo");
            assertThat(capturedTracking.getParcelId()).isEqualTo(parcelId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when parcel not found")
        void shouldThrowExceptionWhenParcelNotFound() {
            UpdateParcelStatusRequest request = new UpdateParcelStatusRequest(
                    ParcelStatus.EN_TRANSITO, null, null
            );
            given(parcelRepository.findById(parcelId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> parcelService.updateStatus(parcelId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(parcelRepository, never()).save(any());
            verify(trackingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getTracking() tests")
    class GetTrackingTests {

        @Test
        @DisplayName("Should return tracking history ordered by timestamp desc")
        void shouldReturnTrackingHistory() {
            ParcelTracking tracking1 = new ParcelTracking();
            tracking1.setParcelId(parcelId);
            tracking1.setStatus(ParcelStatus.RECIBIDO);
            tracking1.setTimestamp(LocalDateTime.now().minusDays(2));

            ParcelTracking tracking2 = new ParcelTracking();
            tracking2.setParcelId(parcelId);
            tracking2.setStatus(ParcelStatus.EN_TRANSITO);
            tracking2.setTimestamp(LocalDateTime.now().minusDays(1));

            ParcelTrackingResponse resp1 = new ParcelTrackingResponse(UUID.randomUUID(), parcelId, ParcelStatus.RECIBIDO, null, tracking1.getTimestamp(), null);
            ParcelTrackingResponse resp2 = new ParcelTrackingResponse(UUID.randomUUID(), parcelId, ParcelStatus.EN_TRANSITO, null, tracking2.getTimestamp(), null);

            given(parcelRepository.existsById(parcelId)).willReturn(true);
            given(trackingRepository.findByParcelIdOrderByTimestampDesc(parcelId))
                    .willReturn(List.of(tracking2, tracking1));
            given(trackingMapper.toResponse(tracking2)).willReturn(resp2);
            given(trackingMapper.toResponse(tracking1)).willReturn(resp1);

            List<ParcelTrackingResponse> result = parcelService.getTracking(parcelId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).status()).isEqualTo(ParcelStatus.EN_TRANSITO);
        }

        @Test
        @DisplayName("Should return empty list when parcel has no tracking records")
        void shouldReturnEmptyListWhenNoTracking() {
            given(parcelRepository.existsById(parcelId)).willReturn(true);
            given(trackingRepository.findByParcelIdOrderByTimestampDesc(parcelId))
                    .willReturn(List.of());

            List<ParcelTrackingResponse> result = parcelService.getTracking(parcelId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when parcel not found")
        void shouldThrowExceptionWhenParcelNotFound() {
            given(parcelRepository.existsById(parcelId)).willReturn(false);

            assertThatThrownBy(() -> parcelService.getTracking(parcelId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(trackingRepository, never()).findByParcelIdOrderByTimestampDesc(any());
        }

        @Test
        @DisplayName("Should call trackingRepository with correct parcelId")
        void shouldCallTrackingRepositoryWithCorrectId() {
            ParcelTracking tracking = new ParcelTracking();
            tracking.setParcelId(parcelId);
            tracking.setStatus(ParcelStatus.RECIBIDO);
            tracking.setTimestamp(LocalDateTime.now());

            ParcelTrackingResponse resp = new ParcelTrackingResponse(UUID.randomUUID(), parcelId, ParcelStatus.RECIBIDO, null, LocalDateTime.now(), null);

            given(parcelRepository.existsById(parcelId)).willReturn(true);
            given(trackingRepository.findByParcelIdOrderByTimestampDesc(parcelId))
                    .willReturn(List.of(tracking));
            given(trackingMapper.toResponse(tracking)).willReturn(resp);

            parcelService.getTracking(parcelId);

            verify(trackingRepository).findByParcelIdOrderByTimestampDesc(parcelId);
        }
    }

    @Nested
    @DisplayName("findByTrackingCode() tests")
    class FindByTrackingCodeTests {

        @Test
        @DisplayName("Should return parcel by tracking code")
        void shouldReturnParcelByTrackingCode() {
            given(parcelRepository.findByTrackingCode("PCL-20240101120000-1234"))
                    .willReturn(Optional.of(testParcel));
            given(parcelMapper.toResponse(testParcel)).willReturn(testParcelResponse);

            ParcelResponse result = parcelService.findByTrackingCode("PCL-20240101120000-1234");

            assertThat(result).isNotNull();
            assertThat(result.trackingCode()).isEqualTo("PCL-20240101120000-1234");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when tracking code not found")
        void shouldThrowExceptionWhenTrackingCodeNotFound() {
            given(parcelRepository.findByTrackingCode("INVALID-CODE"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> parcelService.findByTrackingCode("INVALID-CODE"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should include tracking code in error message when not found")
        void shouldIncludeTrackingCodeInErrorMessage() {
            String invalidCode = "PCL-NONEXISTENT";
            given(parcelRepository.findByTrackingCode(invalidCode))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> parcelService.findByTrackingCode(invalidCode))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(invalidCode);
        }
    }

    @Nested
    @DisplayName("findById() tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return parcel when found by id")
        void shouldReturnParcelWhenFound() {
            given(parcelRepository.findById(parcelId)).willReturn(Optional.of(testParcel));
            given(parcelMapper.toResponse(testParcel)).willReturn(testParcelResponse);

            ParcelResponse result = parcelService.findById(parcelId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(parcelId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when parcel not found by id")
        void shouldThrowExceptionWhenParcelNotFoundById() {
            given(parcelRepository.findById(parcelId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> parcelService.findById(parcelId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
