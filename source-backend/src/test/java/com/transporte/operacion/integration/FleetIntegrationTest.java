package com.transporte.operacion.integration;

import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.operacion.dto.FleetRequest;
import com.transporte.operacion.dto.FleetResponse;
import com.transporte.operacion.entity.Fleet;
import com.transporte.operacion.repository.FleetRepository;
import com.transporte.operacion.service.FleetService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the Fleet lifecycle.
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
@DisplayName("Fleet Integration Tests")
class FleetIntegrationTest {

    @Autowired private FleetService fleetService;
    @Autowired private FleetRepository fleetRepository;

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private FleetRequest buildRequest(String name, String description, boolean active) {
        return new FleetRequest(name, description, active);
    }

    // ---------------------------------------------------------------------------
    // CREATE
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should create a fleet and persist it in the database")
    void shouldCreateFleet() {
        FleetRequest request = buildRequest("Flota Norte", "Rutas del norte del país", true);

        FleetResponse created = fleetService.create(request);

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Flota Norte");
        assertThat(created.description()).isEqualTo("Rutas del norte del país");
        assertThat(created.active()).isTrue();
        assertThat(created.createdAt()).isNotNull();

        assertThat(fleetRepository.existsById(created.id())).isTrue();
    }

    @Test
    @DisplayName("Should create a fleet with null description")
    void shouldCreateFleetWithNullDescription() {
        FleetRequest request = buildRequest("Flota Sur", null, true);

        FleetResponse created = fleetService.create(request);

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Flota Sur");
        assertThat(created.description()).isNull();
    }

    @Test
    @DisplayName("Should throw BusinessException when fleet name already exists")
    void shouldThrowWhenNameDuplicated() {
        fleetService.create(buildRequest("Flota Única", "desc", true));

        assertThatThrownBy(() -> fleetService.create(buildRequest("Flota Única", "otra", true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Flota Única");
    }

    @Test
    @DisplayName("Should throw BusinessException for duplicate name case-insensitive")
    void shouldThrowForDuplicateNameCaseInsensitive() {
        fleetService.create(buildRequest("Flota Centro", "desc", true));

        assertThatThrownBy(() -> fleetService.create(buildRequest("flota centro", "otra", true)))
                .isInstanceOf(BusinessException.class);
    }

    // ---------------------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should find fleet by ID after creation")
    void shouldFindFleetById() {
        FleetResponse created = fleetService.create(buildRequest("Flota Este", "Rutas del este", true));

        FleetResponse found = fleetService.findById(created.id());

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.name()).isEqualTo("Flota Este");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when fleet ID does not exist")
    void shouldThrowWhenFleetNotFound() {
        UUID nonExistent = UUID.randomUUID();

        assertThatThrownBy(() -> fleetService.findById(nonExistent))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Fleet");
    }

    @Test
    @DisplayName("Should return paginated list of all fleets")
    void shouldReturnPaginatedFleets() {
        fleetService.create(buildRequest("Flota A", null, true));
        fleetService.create(buildRequest("Flota B", null, true));
        fleetService.create(buildRequest("Flota C", null, false));

        PageResponse<FleetResponse> page = fleetService.findAll(PageRequest.of(0, 10));

        assertThat(page.content()).hasSize(3);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.first()).isTrue();
    }

    @Test
    @DisplayName("Should return empty page when no fleets exist")
    void shouldReturnEmptyPageWhenNoFleets() {
        PageResponse<FleetResponse> page = fleetService.findAll(PageRequest.of(0, 10));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    // ---------------------------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should update fleet name and description")
    void shouldUpdateFleet() {
        FleetResponse created = fleetService.create(buildRequest("Flota Vieja", "desc vieja", true));

        FleetRequest updateRequest = buildRequest("Flota Nueva", "desc nueva", true);
        FleetResponse updated = fleetService.update(created.id(), updateRequest);

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo("Flota Nueva");
        assertThat(updated.description()).isEqualTo("desc nueva");
    }

    @Test
    @DisplayName("Should deactivate fleet on update")
    void shouldDeactivateFleetViaUpdate() {
        FleetResponse created = fleetService.create(buildRequest("Flota Activa", null, true));

        FleetResponse updated = fleetService.update(created.id(), buildRequest("Flota Activa", null, false));

        assertThat(updated.active()).isFalse();
    }

    @Test
    @DisplayName("Should allow updating fleet to same name (no duplicate error)")
    void shouldAllowUpdatingWithSameName() {
        FleetResponse created = fleetService.create(buildRequest("Flota X", "desc", true));

        FleetResponse updated = fleetService.update(created.id(), buildRequest("Flota X", "desc actualizada", true));

        assertThat(updated.description()).isEqualTo("desc actualizada");
    }

    @Test
    @DisplayName("Should throw BusinessException when updating to existing name of another fleet")
    void shouldThrowWhenUpdatingToExistingName() {
        fleetService.create(buildRequest("Flota Existente", null, true));
        FleetResponse second = fleetService.create(buildRequest("Flota Segunda", null, true));

        assertThatThrownBy(() -> fleetService.update(second.id(), buildRequest("Flota Existente", null, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Flota Existente");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent fleet")
    void shouldThrowWhenUpdatingNonExistentFleet() {
        assertThatThrownBy(() -> fleetService.update(UUID.randomUUID(), buildRequest("X", null, true)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Fleet");
    }

    // ---------------------------------------------------------------------------
    // DELETE (soft delete)
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should soft-delete a fleet (mark active=false)")
    void shouldSoftDeleteFleet() {
        FleetResponse created = fleetService.create(buildRequest("Flota Eliminar", null, true));

        fleetService.delete(created.id());

        Fleet inDb = fleetRepository.findById(created.id()).orElseThrow();
        assertThat(inDb.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent fleet")
    void shouldThrowWhenDeletingNonExistentFleet() {
        assertThatThrownBy(() -> fleetService.delete(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Fleet");
    }
}
