package com.transporte.siat.service;

import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.siat.dto.SiatConfigRequest;
import com.transporte.siat.dto.SiatConfigResponse;
import com.transporte.siat.entity.SiatConfig;
import com.transporte.siat.mapper.SiatConfigMapper;
import com.transporte.siat.repository.SiatConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiatConfigService Unit Tests")
class SiatConfigServiceTest {

    @Mock private SiatConfigRepository configRepository;
    @Mock private SiatConfigMapper configMapper;

    @InjectMocks
    private SiatConfigService configService;

    private UUID configId;
    private SiatConfig testConfig;
    private SiatConfigResponse testResponse;
    private SiatConfigRequest testRequest;

    @BeforeEach
    void setUp() {
        configId = UUID.randomUUID();
        testConfig = buildConfig(configId, true);
        testResponse = buildResponse(configId, true);
        testRequest = new SiatConfigRequest(
                "1020304050", "Empresa Transporte SRL", "SISTEMA01",
                "461000", 0, null, "Av. Siempre Viva 123", "La Paz",
                "22123456", 2, 2
        );
    }

    private SiatConfig buildConfig(UUID id, boolean activo) {
        SiatConfig config = new SiatConfig();
        try {
            var idField = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(config, id);
        } catch (Exception e) { /* ignore */ }
        config.setNit("1020304050");
        config.setRazonSocial("Empresa Transporte SRL");
        config.setCodigoSistema("SISTEMA01");
        config.setCodigoActividad("461000");
        config.setCodigoSucursal(0);
        config.setCodigoPuntoVenta(null);
        config.setCodigoAmbiente(2);
        config.setCodigoModalidad(2);
        config.setActivo(activo);
        return config;
    }

    private SiatConfigResponse buildResponse(UUID id, boolean activo) {
        return new SiatConfigResponse(id, "1020304050", "Empresa Transporte SRL",
                "SISTEMA01", "461000", 0, null, "Av. Siempre Viva 123",
                "La Paz", "22123456", 2, 2, activo, LocalDateTime.now());
    }

    @Nested
    @DisplayName("findAll() tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return mapped list of all configs")
        void shouldReturnAllConfigs() {
            given(configRepository.findAll()).willReturn(List.of(testConfig));
            given(configMapper.toResponse(testConfig)).willReturn(testResponse);

            List<SiatConfigResponse> result = configService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).nit()).isEqualTo("1020304050");
        }

        @Test
        @DisplayName("Should return empty list when no configs exist")
        void shouldReturnEmptyList() {
            given(configRepository.findAll()).willReturn(List.of());

            List<SiatConfigResponse> result = configService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById() tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return config when found")
        void shouldReturnConfigWhenFound() {
            given(configRepository.findById(configId)).willReturn(Optional.of(testConfig));
            given(configMapper.toResponse(testConfig)).willReturn(testResponse);

            SiatConfigResponse result = configService.findById(configId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(configId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            given(configRepository.findById(configId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> configService.findById(configId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create() tests")
    class CreateTests {

        @Test
        @DisplayName("Should save and return mapped config")
        void shouldCreateConfig() {
            given(configMapper.toEntity(testRequest)).willReturn(testConfig);
            given(configRepository.save(testConfig)).willReturn(testConfig);
            given(configMapper.toResponse(testConfig)).willReturn(testResponse);

            SiatConfigResponse result = configService.create(testRequest);

            assertThat(result).isNotNull();
            verify(configRepository).save(testConfig);
        }

        @Test
        @DisplayName("Should call mapper.toEntity before saving")
        void shouldCallMapperBeforeSaving() {
            given(configMapper.toEntity(testRequest)).willReturn(testConfig);
            given(configRepository.save(any())).willReturn(testConfig);
            given(configMapper.toResponse(any())).willReturn(testResponse);

            configService.create(testRequest);

            verify(configMapper).toEntity(testRequest);
        }
    }

    @Nested
    @DisplayName("update() tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update existing config")
        void shouldUpdateConfig() {
            given(configRepository.findById(configId)).willReturn(Optional.of(testConfig));
            given(configRepository.save(testConfig)).willReturn(testConfig);
            given(configMapper.toResponse(testConfig)).willReturn(testResponse);

            SiatConfigResponse result = configService.update(configId, testRequest);

            assertThat(result).isNotNull();
            verify(configMapper).updateEntity(testConfig, testRequest);
            verify(configRepository).save(testConfig);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when config not found")
        void shouldThrowWhenNotFound() {
            given(configRepository.findById(configId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> configService.update(configId, testRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(configRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("toggleActivo() tests")
    class ToggleActivoTests {

        @Test
        @DisplayName("Should toggle active config to inactive")
        void shouldToggleActiveToInactive() {
            testConfig.setActivo(true);
            given(configRepository.findById(configId)).willReturn(Optional.of(testConfig));

            configService.toggleActivo(configId);

            ArgumentCaptor<SiatConfig> captor = ArgumentCaptor.forClass(SiatConfig.class);
            verify(configRepository).save(captor.capture());
            assertThat(captor.getValue().getActivo()).isFalse();
        }

        @Test
        @DisplayName("Should toggle inactive config to active")
        void shouldToggleInactiveToActive() {
            testConfig.setActivo(false);
            given(configRepository.findById(configId)).willReturn(Optional.of(testConfig));

            configService.toggleActivo(configId);

            ArgumentCaptor<SiatConfig> captor = ArgumentCaptor.forClass(SiatConfig.class);
            verify(configRepository).save(captor.capture());
            assertThat(captor.getValue().getActivo()).isTrue();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when config not found")
        void shouldThrowWhenNotFound() {
            given(configRepository.findById(configId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> configService.toggleActivo(configId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getConfigActivaByTenant() tests")
    class GetConfigActivaByTenantTests {

        @Test
        @DisplayName("Should return active config for tenant")
        void shouldReturnActiveConfig() {
            given(configRepository.findFirstByTenantIdAndActivoTrue("tenant-abc"))
                    .willReturn(Optional.of(testConfig));

            SiatConfig result = configService.getConfigActivaByTenant("tenant-abc");

            assertThat(result).isNotNull();
            assertThat(result.getNit()).isEqualTo("1020304050");
        }

        @Test
        @DisplayName("Should throw BusinessException when no active config for tenant")
        void shouldThrowWhenNoActiveConfig() {
            given(configRepository.findFirstByTenantIdAndActivoTrue("tenant-xyz"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> configService.getConfigActivaByTenant("tenant-xyz"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No existe configuración SIAT activa");
        }
    }

    @Nested
    @DisplayName("getById() tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return entity when found")
        void shouldReturnEntityWhenFound() {
            given(configRepository.findById(configId)).willReturn(Optional.of(testConfig));

            SiatConfig result = configService.getById(configId);

            assertThat(result).isNotNull();
            assertThat(result.getNit()).isEqualTo("1020304050");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            given(configRepository.findById(configId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> configService.getById(configId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
