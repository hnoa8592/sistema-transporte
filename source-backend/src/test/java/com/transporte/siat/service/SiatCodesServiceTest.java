package com.transporte.siat.service;

import com.transporte.core.exception.BusinessException;
import com.transporte.siat.client.SiatCodesClient;
import com.transporte.siat.client.SiatSoapResponse;
import com.transporte.siat.dto.SiatCufdResponse;
import com.transporte.siat.dto.SiatCuisResponse;
import com.transporte.siat.entity.SiatConfig;
import com.transporte.siat.entity.SiatCufd;
import com.transporte.siat.entity.SiatCuis;
import com.transporte.siat.mapper.SiatCufdMapper;
import com.transporte.siat.mapper.SiatCuisMapper;
import com.transporte.siat.repository.SiatCufdRepository;
import com.transporte.siat.repository.SiatCuisRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiatCodesService Unit Tests")
class SiatCodesServiceTest {

    @Mock private SiatCodesClient codesClient;
    @Mock private SiatCuisRepository cuisRepository;
    @Mock private SiatCufdRepository cufdRepository;
    @Mock private SiatCuisMapper cuisMapper;
    @Mock private SiatCufdMapper cufdMapper;

    @InjectMocks
    private SiatCodesService codesService;

    private UUID configId;
    private SiatConfig config;
    private SiatCuis activeCuis;
    private SiatCufd activeCufd;

    @BeforeEach
    void setUp() {
        configId = UUID.randomUUID();
        config = buildConfig(configId);
        activeCuis = buildCuis(UUID.randomUUID(), config, "CUIS-TEST-001");
        activeCufd = buildCufd(UUID.randomUUID(), config, "CUFD-TEST-ABCDEFGHIJ", activeCuis);
    }

    private SiatConfig buildConfig(UUID id) {
        SiatConfig c = new SiatConfig();
        try {
            var f = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception e) { /* ignore */ }
        c.setNit("1020304050");
        c.setCodigoSistema("SISTEMA01");
        c.setCodigoSucursal(0);
        c.setCodigoPuntoVenta(null);
        c.setRazonSocial("Transporte SRL");
        c.setCodigoActividad("461000");
        c.setCodigoAmbiente(2);
        c.setCodigoModalidad(2);
        c.setActivo(true);
        return c;
    }

    private SiatCuis buildCuis(UUID id, SiatConfig cfg, String cuisCode) {
        SiatCuis cuis = new SiatCuis();
        try {
            var f = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(cuis, id);
        } catch (Exception e) { /* ignore */ }
        cuis.setSiatConfig(cfg);
        cuis.setCuis(cuisCode);
        cuis.setFechaVigencia(LocalDateTime.now().plusMonths(6));
        cuis.setCodigoSucursal(0);
        cuis.setCodigoPuntoVenta(null);
        cuis.setActivo(true);
        return cuis;
    }

    private SiatCufd buildCufd(UUID id, SiatConfig cfg, String cufdCode, SiatCuis cuis) {
        SiatCufd cufd = new SiatCufd();
        try {
            var f = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(cufd, id);
        } catch (Exception e) { /* ignore */ }
        cufd.setSiatConfig(cfg);
        cufd.setCufd(cufdCode);
        cufd.setCodigoControl("CTRL123");
        cufd.setCodigoParaQr("QR-DATA");
        cufd.setFechaVigencia(LocalDateTime.now().plusHours(23));
        cufd.setCodigoSucursal(0);
        cufd.setCodigoPuntoVenta(null);
        cufd.setActivo(true);
        return cufd;
    }

    // ==================== CUIS ====================

    @Nested
    @DisplayName("obtenerCuis() tests")
    class ObtenerCuisTests {

        @Test
        @DisplayName("Should obtain CUIS from SIN and persist it")
        void shouldObtainAndPersistCuis() {
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true)
                    .datos(Map.of("cuis", "CUIS-NEW-001", "fechaVigencia", "2026-09-11T00:00:00"))
                    .mensaje("Operación exitosa")
                    .build();

            SiatCuisResponse expectedResponse = new SiatCuisResponse(
                    UUID.randomUUID(), "CUIS-NEW-001",
                    LocalDateTime.now().plusMonths(6),
                    0, null, true, true, LocalDateTime.now());

            given(codesClient.obtenerCuis(anyString(), anyString(), anyInt(), any()))
                    .willReturn(soapResp);
            given(cuisRepository.findVigente(configId, 0, null)).willReturn(Optional.empty());
            given(cuisRepository.save(any(SiatCuis.class))).willReturn(activeCuis);
            given(cuisMapper.toResponse(activeCuis)).willReturn(expectedResponse);

            SiatCuisResponse result = codesService.obtenerCuis(config);

            assertThat(result).isNotNull();
            assertThat(result.cuis()).isEqualTo("CUIS-NEW-001");
            verify(cuisRepository).save(any(SiatCuis.class));
        }

        @Test
        @DisplayName("Should deactivate existing CUIS before persisting new one")
        void shouldDeactivateExistingCuisBeforePersisting() {
            SiatCuis oldCuis = buildCuis(UUID.randomUUID(), config, "OLD-CUIS");
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true)
                    .datos(Map.of("cuis", "NEW-CUIS", "fechaVigencia", "2026-09-11T00:00:00"))
                    .mensaje("OK")
                    .build();
            SiatCuisResponse resp = new SiatCuisResponse(UUID.randomUUID(), "NEW-CUIS",
                    LocalDateTime.now().plusMonths(6), 0, null, true, true, LocalDateTime.now());

            given(codesClient.obtenerCuis(anyString(), anyString(), anyInt(), any()))
                    .willReturn(soapResp);
            given(cuisRepository.findVigente(configId, 0, null)).willReturn(Optional.of(oldCuis));
            given(cuisRepository.save(any(SiatCuis.class))).willReturn(activeCuis);
            given(cuisMapper.toResponse(any())).willReturn(resp);

            codesService.obtenerCuis(config);

            // oldCuis should have been saved with activo=false
            verify(cuisRepository, times(2)).save(any(SiatCuis.class));
            assertThat(oldCuis.getActivo()).isFalse();
        }

        @Test
        @DisplayName("Should persist new CUIS with activo=true")
        void shouldPersistNewCuisAsActive() {
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true)
                    .datos(Map.of("cuis", "CUIS-ACTIVE", "fechaVigencia", "2026-09-11T00:00:00"))
                    .mensaje("OK")
                    .build();

            given(codesClient.obtenerCuis(anyString(), anyString(), anyInt(), any()))
                    .willReturn(soapResp);
            given(cuisRepository.findVigente(configId, 0, null)).willReturn(Optional.empty());
            given(cuisRepository.save(any(SiatCuis.class))).willReturn(activeCuis);
            given(cuisMapper.toResponse(any())).willReturn(
                    new SiatCuisResponse(UUID.randomUUID(), "CUIS-ACTIVE",
                            LocalDateTime.now().plusMonths(6), 0, null, true, true, LocalDateTime.now()));

            codesService.obtenerCuis(config);

            ArgumentCaptor<SiatCuis> captor = ArgumentCaptor.forClass(SiatCuis.class);
            verify(cuisRepository).save(captor.capture());
            assertThat(captor.getValue().getActivo()).isTrue();
            assertThat(captor.getValue().getCuis()).isEqualTo("CUIS-ACTIVE");
        }

        @Test
        @DisplayName("Should throw BusinessException when SIN returns error")
        void shouldThrowWhenSinReturnsError() {
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(false)
                    .datos(Map.of())
                    .mensaje("Error de autenticación")
                    .build();

            given(codesClient.obtenerCuis(anyString(), anyString(), anyInt(), any()))
                    .willReturn(soapResp);

            assertThatThrownBy(() -> codesService.obtenerCuis(config))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Error obteniendo CUIS");
        }

        @Test
        @DisplayName("Should throw BusinessException when SIN returns null cuis data")
        void shouldThrowWhenNullCuisData() {
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true)
                    .datos(Map.of()) // no "cuis" key
                    .mensaje("OK")
                    .build();

            given(codesClient.obtenerCuis(anyString(), anyString(), anyInt(), any()))
                    .willReturn(soapResp);

            assertThatThrownBy(() -> codesService.obtenerCuis(config))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Error obteniendo CUIS");
        }

        @Test
        @DisplayName("Should use +24h fallback when fechaVigencia is blank")
        void shouldUseFallbackWhenFechaVigenciaBlank() {
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true)
                    .datos(Map.of("cuis", "CUIS-FB"))
                    .mensaje("OK")
                    .build();

            given(codesClient.obtenerCuis(anyString(), anyString(), anyInt(), any()))
                    .willReturn(soapResp);
            given(cuisRepository.findVigente(configId, 0, null)).willReturn(Optional.empty());
            given(cuisRepository.save(any(SiatCuis.class))).willReturn(activeCuis);
            given(cuisMapper.toResponse(any())).willReturn(
                    new SiatCuisResponse(UUID.randomUUID(), "CUIS-FB",
                            LocalDateTime.now().plusHours(24), 0, null, true, true, LocalDateTime.now()));

            // Should not throw
            SiatCuisResponse result = codesService.obtenerCuis(config);
            assertThat(result).isNotNull();

            ArgumentCaptor<SiatCuis> captor = ArgumentCaptor.forClass(SiatCuis.class);
            verify(cuisRepository).save(captor.capture());
            // vigencia should be roughly now + 24h (within tolerance)
            assertThat(captor.getValue().getFechaVigencia()).isAfter(LocalDateTime.now().plusHours(23));
        }
    }

    @Nested
    @DisplayName("getCuisVigente() tests")
    class GetCuisVigenteTests {

        @Test
        @DisplayName("Should return existing vigente CUIS")
        void shouldReturnVigenteCuis() {
            given(cuisRepository.findVigente(configId, 0, null))
                    .willReturn(Optional.of(activeCuis));

            SiatCuis result = codesService.getCuisVigente(configId, 0, null);

            assertThat(result).isNotNull();
            assertThat(result.getCuis()).isEqualTo("CUIS-TEST-001");
        }

        @Test
        @DisplayName("Should throw BusinessException when no vigente CUIS exists")
        void shouldThrowWhenNoCuisVigente() {
            given(cuisRepository.findVigente(configId, 0, null))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> codesService.getCuisVigente(configId, 0, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No existe CUIS vigente");
        }
    }

    // ==================== CUFD ====================

    @Nested
    @DisplayName("obtenerCufd() tests")
    class ObtenerCufdTests {

        @Test
        @DisplayName("Should obtain CUFD from SIN and persist it")
        void shouldObtainAndPersistCufd() {
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true)
                    .datos(Map.of(
                            "cufd", "CUFD-NEW-ABCDEFGHIJ",
                            "codigoControl", "CTRL-NEW",
                            "codigoParaQr", "QR-NEW",
                            "fechaVigencia", "2026-03-11T23:59:00"))
                    .mensaje("OK")
                    .build();

            SiatCufdResponse expectedResponse = new SiatCufdResponse(
                    UUID.randomUUID(), "CUFD-NEW-ABCDEFGHIJ", "CTRL-NEW", "QR-NEW",
                    LocalDateTime.now().plusHours(23), 0, null, true, true, LocalDateTime.now());

            given(cuisRepository.findVigente(configId, 0, null)).willReturn(Optional.of(activeCuis));
            given(codesClient.obtenerCufd(anyString(), anyString(), anyString(), anyInt(), any()))
                    .willReturn(soapResp);
            given(cufdRepository.findVigente(configId, 0, null)).willReturn(Optional.empty());
            given(cufdRepository.save(any(SiatCufd.class))).willReturn(activeCufd);
            given(cufdMapper.toResponse(activeCufd)).willReturn(expectedResponse);

            SiatCufdResponse result = codesService.obtenerCufd(config);

            assertThat(result).isNotNull();
            assertThat(result.cufd()).isEqualTo("CUFD-NEW-ABCDEFGHIJ");
            verify(cufdRepository).save(any(SiatCufd.class));
        }

        @Test
        @DisplayName("Should deactivate old CUFD before persisting new one")
        void shouldDeactivateOldCufd() {
            SiatCufd oldCufd = buildCufd(UUID.randomUUID(), config, "OLD-CUFD-CODE", activeCuis);
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true)
                    .datos(Map.of("cufd", "NEW-CUFD", "codigoControl", "CTRL",
                            "codigoParaQr", "QR", "fechaVigencia", "2026-03-11T23:59:00"))
                    .mensaje("OK")
                    .build();

            given(cuisRepository.findVigente(configId, 0, null)).willReturn(Optional.of(activeCuis));
            given(codesClient.obtenerCufd(anyString(), anyString(), anyString(), anyInt(), any()))
                    .willReturn(soapResp);
            given(cufdRepository.findVigente(configId, 0, null)).willReturn(Optional.of(oldCufd));
            given(cufdRepository.save(any(SiatCufd.class))).willReturn(activeCufd);
            given(cufdMapper.toResponse(any())).willReturn(
                    new SiatCufdResponse(UUID.randomUUID(), "NEW-CUFD", "CTRL", "QR",
                            LocalDateTime.now().plusHours(23), 0, null, true, true, LocalDateTime.now()));

            codesService.obtenerCufd(config);

            verify(cufdRepository, times(2)).save(any(SiatCufd.class));
            assertThat(oldCufd.getActivo()).isFalse();
        }

        @Test
        @DisplayName("Should throw BusinessException when no vigente CUIS for CUFD request")
        void shouldThrowWhenNoCuisForCufd() {
            given(cuisRepository.findVigente(configId, 0, null)).willReturn(Optional.empty());

            assertThatThrownBy(() -> codesService.obtenerCufd(config))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No existe CUIS vigente");
        }

        @Test
        @DisplayName("Should throw BusinessException when SIN returns error for CUFD")
        void shouldThrowWhenSinErrorForCufd() {
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(false)
                    .datos(Map.of())
                    .mensaje("NIT inválido")
                    .build();

            given(cuisRepository.findVigente(configId, 0, null)).willReturn(Optional.of(activeCuis));
            given(codesClient.obtenerCufd(anyString(), anyString(), anyString(), anyInt(), any()))
                    .willReturn(soapResp);

            assertThatThrownBy(() -> codesService.obtenerCufd(config))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Error obteniendo CUFD");
        }

        @Test
        @DisplayName("Should persist CUFD with codigoControl from SIN response")
        void shouldPersistCufdWithCodigoControl() {
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true)
                    .datos(Map.of("cufd", "CUFD-X", "codigoControl", "CTRL-X",
                            "fechaVigencia", "2026-03-11T23:59:00"))
                    .mensaje("OK")
                    .build();

            given(cuisRepository.findVigente(configId, 0, null)).willReturn(Optional.of(activeCuis));
            given(codesClient.obtenerCufd(anyString(), anyString(), anyString(), anyInt(), any()))
                    .willReturn(soapResp);
            given(cufdRepository.findVigente(configId, 0, null)).willReturn(Optional.empty());
            given(cufdRepository.save(any(SiatCufd.class))).willReturn(activeCufd);
            given(cufdMapper.toResponse(any())).willReturn(
                    new SiatCufdResponse(UUID.randomUUID(), "CUFD-X", "CTRL-X", null,
                            LocalDateTime.now().plusHours(23), 0, null, true, true, LocalDateTime.now()));

            codesService.obtenerCufd(config);

            ArgumentCaptor<SiatCufd> captor = ArgumentCaptor.forClass(SiatCufd.class);
            verify(cufdRepository).save(captor.capture());
            assertThat(captor.getValue().getCodigoControl()).isEqualTo("CTRL-X");
            assertThat(captor.getValue().getActivo()).isTrue();
        }
    }

    @Nested
    @DisplayName("getCufdVigente() tests")
    class GetCufdVigenteTests {

        @Test
        @DisplayName("Should return existing vigente CUFD")
        void shouldReturnVigenteCufd() {
            given(cufdRepository.findVigente(configId, 0, null))
                    .willReturn(Optional.of(activeCufd));

            SiatCufd result = codesService.getCufdVigente(configId, 0, null);

            assertThat(result).isNotNull();
            assertThat(result.getCufd()).isEqualTo("CUFD-TEST-ABCDEFGHIJ");
        }

        @Test
        @DisplayName("Should throw BusinessException when no vigente CUFD exists")
        void shouldThrowWhenNoCufdVigente() {
            given(cufdRepository.findVigente(configId, 0, null))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> codesService.getCufdVigente(configId, 0, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No existe CUFD vigente");
        }
    }
}
