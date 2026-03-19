package com.transporte.operacion.integration;

import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.DriverRequest;
import com.transporte.operacion.dto.DriverResponse;
import com.transporte.operacion.entity.Driver;
import com.transporte.operacion.repository.DriverRepository;
import com.transporte.operacion.service.DriverService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Driver lifecycle.
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
@DisplayName("Driver Integration Tests")
class DriverIntegrationTest {

    @Autowired private DriverService driverService;
    @Autowired private DriverRepository driverRepository;

    private static final LocalDate FUTURE_DATE  = LocalDate.now().plusYears(2);
    private static final LocalDate PAST_DATE    = LocalDate.now().minusDays(10);
    private static final LocalDate NEAR_DATE    = LocalDate.now().plusDays(15);

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private DriverRequest buildRequest(String dni, String firstName, String lastName,
                                       String licenseNumber, String licenseCategory,
                                       LocalDate expiryDate) {
        return new DriverRequest(dni, firstName, lastName, licenseNumber, licenseCategory,
                expiryDate, "70001234", "chofer@test.com", true);
    }

    private DriverRequest defaultRequest(String dni) {
        return buildRequest(dni, "Carlos", "Mamani", "LIC-" + dni, "B", FUTURE_DATE);
    }

    // ---------------------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should create a driver and persist it in the database")
    void shouldCreateDriver() {
        DriverRequest request = buildRequest("12345678", "Carlos", "Mamani",
                "LIC-001", "B", FUTURE_DATE);

        DriverResponse created = driverService.create(request);

        assertThat(created.id()).isNotNull();
        assertThat(created.dni()).isEqualTo("12345678");
        assertThat(created.firstName()).isEqualTo("Carlos");
        assertThat(created.lastName()).isEqualTo("Mamani");
        assertThat(created.licenseNumber()).isEqualTo("LIC-001");
        assertThat(created.licenseCategory()).isEqualTo("B");
        assertThat(created.licenseExpiryDate()).isEqualTo(FUTURE_DATE);
        assertThat(created.phone()).isEqualTo("70001234");
        assertThat(created.email()).isEqualTo("chofer@test.com");
        assertThat(created.active()).isTrue();
        assertThat(created.createdAt()).isNotNull();

        assertThat(driverRepository.existsById(created.id())).isTrue();
    }

    @Test
    @DisplayName("Should create a driver with minimal required fields")
    void shouldCreateDriverWithMinimalFields() {
        DriverRequest request = new DriverRequest(
                "99999999", "Ana", "Lopez", null, null, null, null, null, true);

        DriverResponse created = driverService.create(request);

        assertThat(created.id()).isNotNull();
        assertThat(created.dni()).isEqualTo("99999999");
        assertThat(created.licenseNumber()).isNull();
        assertThat(created.licenseExpiryDate()).isNull();
    }

    @Test
    @DisplayName("Should throw BusinessException when DNI already exists on create")
    void shouldThrowWhenDniDuplicatedOnCreate() {
        driverService.create(defaultRequest("11111111"));

        assertThatThrownBy(() -> driverService.create(defaultRequest("11111111")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("11111111");
    }

    // ---------------------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should find driver by ID after creation")
    void shouldFindDriverById() {
        DriverResponse created = driverService.create(defaultRequest("22222222"));

        DriverResponse found = driverService.findById(created.id());

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.dni()).isEqualTo("22222222");
        assertThat(found.firstName()).isEqualTo("Carlos");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when driver ID does not exist")
    void shouldThrowWhenDriverNotFound() {
        assertThatThrownBy(() -> driverService.findById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Driver");
    }

    @Test
    @DisplayName("Should return only active drivers in paginated list")
    void shouldReturnOnlyActiveDrivers() {
        DriverResponse d1 = driverService.create(defaultRequest("30000001"));
        DriverResponse d2 = driverService.create(defaultRequest("30000002"));
        DriverResponse d3 = driverService.create(defaultRequest("30000003"));

        driverService.delete(d3.id());

        PageResponse<DriverResponse> page = driverService.findAll(PageRequest.of(0, 10));

        assertThat(page.content()).hasSize(2);
        assertThat(page.content()).extracting(DriverResponse::id)
                .containsExactlyInAnyOrder(d1.id(), d2.id());
    }

    @Test
    @DisplayName("Should return empty page when no active drivers exist")
    void shouldReturnEmptyPageWhenNoDrivers() {
        PageResponse<DriverResponse> page = driverService.findAll(PageRequest.of(0, 10));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    @DisplayName("Should respect pagination parameters")
    void shouldPaginateCorrectly() {
        for (int i = 1; i <= 5; i++) {
            driverService.create(defaultRequest("4000000" + i));
        }

        PageResponse<DriverResponse> page = driverService.findAll(PageRequest.of(0, 2));

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isFalse();
    }

    // ---------------------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should update driver fields correctly")
    void shouldUpdateDriver() {
        DriverResponse created = driverService.create(defaultRequest("55555555"));

        DriverRequest updateRequest = buildRequest(
                "55555555", "Pedro", "Quispe", "LIC-UPD", "C", NEAR_DATE);
        DriverResponse updated = driverService.update(created.id(), updateRequest);

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.firstName()).isEqualTo("Pedro");
        assertThat(updated.lastName()).isEqualTo("Quispe");
        assertThat(updated.licenseNumber()).isEqualTo("LIC-UPD");
        assertThat(updated.licenseCategory()).isEqualTo("C");
        assertThat(updated.licenseExpiryDate()).isEqualTo(NEAR_DATE);
    }

    @Test
    @DisplayName("Should allow updating driver with same DNI (no false duplicate error)")
    void shouldAllowUpdateWithSameDni() {
        DriverResponse created = driverService.create(defaultRequest("66666666"));

        DriverRequest updateRequest = buildRequest(
                "66666666", "Carlos Actualizado", "Mamani", "LIC-001", "B", FUTURE_DATE);
        DriverResponse updated = driverService.update(created.id(), updateRequest);

        assertThat(updated.firstName()).isEqualTo("Carlos Actualizado");
    }

    @Test
    @DisplayName("Should throw BusinessException when updating to DNI of another driver")
    void shouldThrowWhenUpdatingToDuplicateDni() {
        driverService.create(defaultRequest("77777777"));
        DriverResponse second = driverService.create(defaultRequest("88888888"));

        DriverRequest conflict = buildRequest(
                "77777777", "Juan", "Garcia", "LIC-X", "A", FUTURE_DATE);

        assertThatThrownBy(() -> driverService.update(second.id(), conflict))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("77777777");
    }

    @Test
    @DisplayName("Should deactivate driver via update")
    void shouldDeactivateDriverViaUpdate() {
        DriverResponse created = driverService.create(defaultRequest("91111111"));

        DriverRequest deactivate = new DriverRequest(
                "91111111", "Carlos", "Mamani", "LIC-91", "B", FUTURE_DATE,
                "70001234", "chofer@test.com", false);
        DriverResponse updated = driverService.update(created.id(), deactivate);

        assertThat(updated.active()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent driver")
    void shouldThrowWhenUpdatingNonExistentDriver() {
        assertThatThrownBy(() -> driverService.update(UUID.randomUUID(), defaultRequest("00000000")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Driver");
    }

    // ---------------------------------------------------------------------------
    // DELETE (soft delete)
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should soft-delete a driver (mark active=false)")
    void shouldSoftDeleteDriver() {
        DriverResponse created = driverService.create(defaultRequest("12121212"));

        driverService.delete(created.id());

        Driver inDb = driverRepository.findById(created.id()).orElseThrow();
        assertThat(inDb.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should still find soft-deleted driver by ID (findById queries all)")
    void shouldFindDeletedDriverById() {
        DriverResponse created = driverService.create(defaultRequest("13131313"));
        driverService.delete(created.id());

        // findById uses findById on repo (no active filter) so it should still return the record
        assertThat(driverRepository.findById(created.id())).isPresent();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent driver")
    void shouldThrowWhenDeletingNonExistentDriver() {
        assertThatThrownBy(() -> driverService.delete(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Driver");
    }

    // ---------------------------------------------------------------------------
    // LICENSE EXPIRY (business logic in component, but repo level verified here)
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should persist past license expiry date")
    void shouldPersistPastExpiryDate() {
        DriverRequest request = buildRequest("14141414", "Rosa", "Torrez",
                "LIC-EXP", "A", PAST_DATE);

        DriverResponse created = driverService.create(request);

        assertThat(created.licenseExpiryDate()).isEqualTo(PAST_DATE);
        assertThat(created.licenseExpiryDate()).isBefore(LocalDate.now());
    }
}
