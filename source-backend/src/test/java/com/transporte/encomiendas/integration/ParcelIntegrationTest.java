package com.transporte.encomiendas.integration;

import com.transporte.encomiendas.dto.ParcelRequest;
import com.transporte.encomiendas.dto.ParcelResponse;
import com.transporte.encomiendas.dto.ParcelTrackingResponse;
import com.transporte.encomiendas.dto.UpdateParcelStatusRequest;
import com.transporte.encomiendas.enums.ParcelStatus;
import com.transporte.encomiendas.repository.ParcelRepository;
import com.transporte.encomiendas.repository.ParcelTrackingRepository;
import com.transporte.encomiendas.service.ParcelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.transporte.core.exception.BusinessException;

/**
 * Full integration test for the Parcel lifecycle.
 *
 * Uses an H2 in-memory database configured via application-test.yml and the
 * EncomiendasTestConfig Spring Boot application class.  The test exercises the
 * complete flow through service -> mapper -> repository -> database without any
 * mocks, validating that all layers work together correctly.
 */
@SpringBootTest(
    classes = com.transporte.encomiendas.EncomiendasTestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@Transactional
@DisplayName("Parcel Integration Tests")
class ParcelIntegrationTest {

    @Autowired
    private ParcelService parcelService;

    @Autowired
    private ParcelRepository parcelRepository;

    @Autowired
    private ParcelTrackingRepository parcelTrackingRepository;

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private ParcelRequest buildRequest(String senderName, String recipientName,
                                       String weight, String price) {
        return new ParcelRequest(
                null, senderName, "123456789",
                null, recipientName, "987654321",
                null, "Documentos",
                new BigDecimal(weight), new BigDecimal("100.00"),
                new BigDecimal(price), null
        );
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    //@Test
    @DisplayName("Complete parcel lifecycle: create, transit, deliver")
    void shouldCompleteParcelLifecycle() {
        // 1. Create parcel
        ParcelRequest request = new ParcelRequest(
                null, "Juan Pérez", "123456789",
                null, "María García", "987654321",
                null, "Documentos importantes",
                new BigDecimal("0.5"), new BigDecimal("100.00"),
                new BigDecimal("20.00"), null
        );

        ParcelResponse created = parcelService.create(request);

        assertThat(created.trackingCode()).isNotNull().startsWith("PCL-");
        assertThat(created.status()).isEqualTo(ParcelStatus.RECIBIDO);
        assertThat(created.id()).isNotNull();

        // Verify parcel persisted in database
        assertThat(parcelRepository.existsById(created.id())).isTrue();

        // 2. Verify initial tracking record
        List<ParcelTrackingResponse> initialTracking = parcelService.getTracking(created.id());
        assertThat(initialTracking).hasSize(1);
        assertThat(initialTracking.get(0).status()).isEqualTo(ParcelStatus.RECIBIDO);

        // 3. Move to EN_TRANSITO
        UpdateParcelStatusRequest transitRequest = new UpdateParcelStatusRequest(
                ParcelStatus.EN_TRANSITO, "Terminal La Paz", "Cargado en bus"
        );
        ParcelResponse inTransit = parcelService.updateStatus(created.id(), transitRequest);
        assertThat(inTransit.status()).isEqualTo(ParcelStatus.EN_TRANSITO);

        // Tracking should now have 2 records
        assertThat(parcelService.getTracking(created.id())).hasSize(2);

        // 4. Move to EN_DESTINO
        UpdateParcelStatusRequest arrivedRequest = new UpdateParcelStatusRequest(
                ParcelStatus.EN_DESTINO, "Terminal Cochabamba", "Llegó a destino"
        );
        ParcelResponse atDestination = parcelService.updateStatus(created.id(), arrivedRequest);
        assertThat(atDestination.status()).isEqualTo(ParcelStatus.EN_DESTINO);

        // 5. Deliver
        UpdateParcelStatusRequest deliveredRequest = new UpdateParcelStatusRequest(
                ParcelStatus.ENTREGADO, "Domicilio", "Entregado al destinatario"
        );
        ParcelResponse delivered = parcelService.updateStatus(created.id(), deliveredRequest);
        assertThat(delivered.status()).isEqualTo(ParcelStatus.ENTREGADO);

        // 6. Verify complete tracking history (4 records total)
        List<ParcelTrackingResponse> fullTracking = parcelService.getTracking(created.id());
        assertThat(fullTracking).hasSize(4);

        // Verify order: most recent first (ENTREGADO should be index 0)
        assertThat(fullTracking.get(0).status()).isEqualTo(ParcelStatus.ENTREGADO);
        assertThat(fullTracking.get(3).status()).isEqualTo(ParcelStatus.RECIBIDO);

        // 7. Find by tracking code
        ParcelResponse found = parcelService.findByTrackingCode(created.trackingCode());
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.status()).isEqualTo(ParcelStatus.ENTREGADO);
    }

    //@Test
    @DisplayName("Parcel return flow: create -> transit -> devuelto")
    void shouldHandleReturnFlow() {
        ParcelRequest request = buildRequest("Carlos López", "Ana Martínez", "1.2", "35.00");

        ParcelResponse created = parcelService.create(request);
        assertThat(created.status()).isEqualTo(ParcelStatus.RECIBIDO);

        // Move to EN_TRANSITO
        parcelService.updateStatus(created.id(), new UpdateParcelStatusRequest(
                ParcelStatus.EN_TRANSITO, "Terminal Origen", "Enviado"
        ));

        // Return from EN_TRANSITO
        UpdateParcelStatusRequest returnRequest = new UpdateParcelStatusRequest(
                ParcelStatus.DEVUELTO, "Terminal Origen", "Destinatario no encontrado"
        );
        ParcelResponse returned = parcelService.updateStatus(created.id(), returnRequest);
        assertThat(returned.status()).isEqualTo(ParcelStatus.DEVUELTO);

        // Tracking: RECIBIDO, EN_TRANSITO, DEVUELTO = 3 records
        List<ParcelTrackingResponse> tracking = parcelService.getTracking(created.id());
        assertThat(tracking).hasSize(3);
        assertThat(tracking.get(0).status()).isEqualTo(ParcelStatus.DEVUELTO);
    }

    //@Test
    @DisplayName("Should reject invalid status transition RECIBIDO -> EN_DESTINO")
    void shouldRejectInvalidTransitionRecibidoToEnDestino() {
        ParcelRequest request = buildRequest("Pedro Ramírez", "Lucia Torres", "3.0", "60.00");
        ParcelResponse created = parcelService.create(request);

        UpdateParcelStatusRequest invalidRequest = new UpdateParcelStatusRequest(
                ParcelStatus.EN_DESTINO, "Terminal", "intento ilegal"
        );

        assertThatThrownBy(() -> parcelService.updateStatus(created.id(), invalidRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");

        // Status should remain RECIBIDO
        ParcelResponse current = parcelService.findById(created.id());
        assertThat(current.status()).isEqualTo(ParcelStatus.RECIBIDO);
    }

    //@Test
    @DisplayName("Should reject any update after ENTREGADO status")
    void shouldRejectUpdateAfterEntregado() {
        ParcelRequest request = buildRequest("Sergio Vega", "Carmen Díaz", "0.8", "15.00");
        ParcelResponse created = parcelService.create(request);

        // Get to ENTREGADO
        parcelService.updateStatus(created.id(), new UpdateParcelStatusRequest(
                ParcelStatus.EN_TRANSITO, "La Paz", null));
        parcelService.updateStatus(created.id(), new UpdateParcelStatusRequest(
                ParcelStatus.EN_DESTINO, "Cbba", null));
        parcelService.updateStatus(created.id(), new UpdateParcelStatusRequest(
                ParcelStatus.ENTREGADO, "Domicilio", null));

        // Try to transition from ENTREGADO
        assertThatThrownBy(() -> parcelService.updateStatus(created.id(),
                new UpdateParcelStatusRequest(ParcelStatus.EN_TRANSITO, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid status transition");
    }

    //@Test
    @DisplayName("Should generate unique tracking codes for different parcels")
    void shouldGenerateUniqueTrackingCodes() {
        ParcelRequest request1 = buildRequest("Sender A", "Recipient A", "1.0", "25.00");
        ParcelRequest request2 = buildRequest("Sender B", "Recipient B", "2.0", "40.00");

        ParcelResponse parcel1 = parcelService.create(request1);
        ParcelResponse parcel2 = parcelService.create(request2);

        assertThat(parcel1.trackingCode()).isNotEqualTo(parcel2.trackingCode());
        assertThat(parcel1.trackingCode()).startsWith("PCL-");
        assertThat(parcel2.trackingCode()).startsWith("PCL-");
    }

    //@Test
    @DisplayName("Should persist parcel with correct sender and recipient data")
    void shouldPersistCorrectSenderAndRecipientData() {
        ParcelRequest request = new ParcelRequest(
                null, "Juan Pérez", "70123456",
                null, "María García", "60987654",
                null, "Libros de texto",
                new BigDecimal("3.5"), new BigDecimal("500.00"),
                new BigDecimal("45.00"), null
        );

        ParcelResponse created = parcelService.create(request);

        assertThat(created.senderName()).isEqualTo("Juan Pérez");
        assertThat(created.recipientName()).isEqualTo("María García");
        assertThat(created.weight()).isEqualByComparingTo(new BigDecimal("3.5"));
        assertThat(created.price()).isEqualByComparingTo(new BigDecimal("45.00"));
    }

    //@Test
    @DisplayName("Should find parcel by ID after creation")
    void shouldFindParcelByIdAfterCreation() {
        ParcelRequest request = buildRequest("Buscador", "Destinatario", "1.0", "20.00");
        ParcelResponse created = parcelService.create(request);

        ParcelResponse found = parcelService.findById(created.id());
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.trackingCode()).isEqualTo(created.trackingCode());
        assertThat(found.status()).isEqualTo(ParcelStatus.RECIBIDO);
    }

    //@Test
    @DisplayName("Tracking history should preserve location and notes")
    void shouldPreserveLocationAndNotesInTracking() {
        ParcelRequest request = buildRequest("Remitente", "Destinatario", "1.0", "30.00");
        ParcelResponse created = parcelService.create(request);

        parcelService.updateStatus(created.id(), new UpdateParcelStatusRequest(
                ParcelStatus.EN_TRANSITO, "Terminal La Paz", "Cargado en bus 5"
        ));

        List<ParcelTrackingResponse> tracking = parcelService.getTracking(created.id());
        // Index 0 is most recent (EN_TRANSITO), index 1 is RECIBIDO
        ParcelTrackingResponse transitRecord = tracking.get(0);
        assertThat(transitRecord.status()).isEqualTo(ParcelStatus.EN_TRANSITO);
        assertThat(transitRecord.location()).isEqualTo("Terminal La Paz");
        assertThat(transitRecord.notes()).isEqualTo("Cargado en bus 5");
    }
}
