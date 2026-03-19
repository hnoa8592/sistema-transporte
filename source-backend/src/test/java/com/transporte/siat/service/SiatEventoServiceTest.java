package com.transporte.siat.service;

import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.siat.client.SiatCodesClient;
import com.transporte.siat.client.SiatSoapResponse;
import com.transporte.siat.dto.SiatEventoRequest;
import com.transporte.siat.dto.SiatEventoResponse;
import com.transporte.siat.entity.SiatConfig;
import com.transporte.siat.entity.SiatCufd;
import com.transporte.siat.entity.SiatCuis;
import com.transporte.siat.entity.SiatEvento;
import com.transporte.siat.mapper.SiatEventoMapper;
import com.transporte.siat.repository.SiatConfigRepository;
import com.transporte.siat.repository.SiatEventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiatEventoService Unit Tests")
class SiatEventoServiceTest {

    @Mock private SiatCodesClient codesClient;
    @Mock private SiatCodesService codesService;
    @Mock private SiatEventoRepository eventoRepository;
    @Mock private SiatConfigRepository configRepository;
    @Mock private SiatEventoMapper eventoMapper;

    @InjectMocks
    private SiatEventoService eventoService;

    private UUID configId;
    private SiatConfig config;
    private SiatCuis cuis;
    private SiatCufd cufd;
    private SiatEventoResponse mockEventoResponse;

    @BeforeEach
    void setUp() {
        configId = UUID.randomUUID();
        config = buildConfig(configId);
        cuis = buildCuis(UUID.randomUUID(), config);
        cufd = buildCufd(UUID.randomUUID(), config);
        mockEventoResponse = buildEventoResponse(UUID.randomUUID());
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
        return c;
    }

    private SiatCuis buildCuis(UUID id, SiatConfig cfg) {
        SiatCuis cuis = new SiatCuis();
        try {
            var f = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(cuis, id);
        } catch (Exception e) { /* ignore */ }
        cuis.setSiatConfig(cfg);
        cuis.setCuis("CUIS-TEST-001");
        cuis.setFechaVigencia(LocalDateTime.now().plusMonths(6));
        cuis.setCodigoSucursal(0);
        cuis.setActivo(true);
        return cuis;
    }

    private SiatCufd buildCufd(UUID id, SiatConfig cfg) {
        SiatCufd cufd = new SiatCufd();
        try {
            var f = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(cufd, id);
        } catch (Exception e) { /* ignore */ }
        cufd.setSiatConfig(cfg);
        cufd.setCufd("CUFD-TEST-ABCDEFGHIJ");
        cufd.setCodigoControl("CTRL123");
        cufd.setFechaVigencia(LocalDateTime.now().plusHours(23));
        cufd.setCodigoSucursal(0);
        cufd.setActivo(true);
        return cufd;
    }

    private SiatEvento buildEvento(UUID id, int codigoEvento) {
        return SiatEvento.builder()
                .siatConfig(config)
                .siatCufd(cufd)
                .codigoEvento(codigoEvento)
                .descripcion("Corte de energía")
                .fechaInicio(LocalDateTime.now().minusHours(2))
                .fechaFin(LocalDateTime.now())
                .codigoSucursal(0)
                .codigoPuntoVenta(null)
                .codigoRecepcion("REC-001")
                .estado("REGISTRADO")
                .build();
    }

    private SiatEventoResponse buildEventoResponse(UUID id) {
        return new SiatEventoResponse(id, 1, "Corte de energía",
                LocalDateTime.now().minusHours(2), LocalDateTime.now(),
                0, null, "REC-001", "REGISTRADO", "OK", LocalDateTime.now());
    }

    @Nested
    @DisplayName("findByConfig() tests")
    class FindByConfigTests {

        @Test
        @DisplayName("Should return paged events for a config")
        void shouldReturnPagedEvents() {
            SiatEvento evento = buildEvento(UUID.randomUUID(), 1);
            Page<SiatEvento> page = new PageImpl<>(List.of(evento), PageRequest.of(0, 10), 1);
            Pageable pageable = PageRequest.of(0, 10);

            given(eventoRepository.findBySiatConfigId(configId, pageable)).willReturn(page);
            given(eventoMapper.toResponse(evento)).willReturn(mockEventoResponse);

            PageResponse<SiatEventoResponse> result = eventoService.findByConfig(configId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty page when no events exist for config")
        void shouldReturnEmptyPage() {
            Page<SiatEvento> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            Pageable pageable = PageRequest.of(0, 10);

            given(eventoRepository.findBySiatConfigId(configId, pageable)).willReturn(emptyPage);

            PageResponse<SiatEventoResponse> result = eventoService.findByConfig(configId, pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("registrar() tests")
    class RegistrarTests {

        @Test
        @DisplayName("Should register event successfully when SIN accepts it")
        void shouldRegisterEventSuccessfully() {
            SiatEventoRequest request = new SiatEventoRequest(
                    configId, 1, "Corte de energía",
                    LocalDateTime.now().minusHours(2), LocalDateTime.now(), null, null);

            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true).datos(Map.of()).codigoRecepcion("REC-001").mensaje("OK").build();
            SiatEvento savedEvento = buildEvento(UUID.randomUUID(), 1);

            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCuisVigente(configId, 0, null)).willReturn(cuis);
            given(codesService.getCufdVigente(configId, 0, null)).willReturn(cufd);
            given(codesClient.registrarEvento(anyString(), anyString(), anyString(),
                    anyInt(), any(), anyInt(), anyString(), anyString(), any())).willReturn(soapResp);
            given(eventoRepository.save(any(SiatEvento.class))).willReturn(savedEvento);
            given(eventoMapper.toResponse(savedEvento)).willReturn(mockEventoResponse);

            SiatEventoResponse result = eventoService.registrar(request);

            assertThat(result).isNotNull();
            assertThat(result.estado()).isEqualTo("REGISTRADO");
            verify(eventoRepository).save(any(SiatEvento.class));
        }

        @Test
        @DisplayName("Should persist event with estado=ERROR when SIN rejects it")
        void shouldPersistEventWithErrorWhenSinRejects() {
            SiatEventoRequest request = new SiatEventoRequest(
                    configId, 2, "Internet caído",
                    LocalDateTime.now().minusHours(1), null, null, null);

            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(false).datos(Map.of()).codigoRecepcion(null).mensaje("Error SIAT").build();
            SiatEvento savedEvento = buildEvento(UUID.randomUUID(), 2);
            savedEvento.setEstado("ERROR");

            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCuisVigente(configId, 0, null)).willReturn(cuis);
            given(codesService.getCufdVigente(configId, 0, null)).willReturn(cufd);
            given(codesClient.registrarEvento(anyString(), anyString(), anyString(),
                    anyInt(), any(), anyInt(), anyString(), anyString(), any())).willReturn(soapResp);
            given(eventoRepository.save(any(SiatEvento.class))).willReturn(savedEvento);
            SiatEventoResponse errorResp = new SiatEventoResponse(UUID.randomUUID(), 2, "Internet caído",
                    LocalDateTime.now().minusHours(1), null, 0, null, null, "ERROR", "Error SIAT", LocalDateTime.now());
            given(eventoMapper.toResponse(savedEvento)).willReturn(errorResp);

            SiatEventoResponse result = eventoService.registrar(request);

            // Should still save (just with ERROR state) — not throw
            assertThat(result).isNotNull();
            ArgumentCaptor<SiatEvento> captor = ArgumentCaptor.forClass(SiatEvento.class);
            verify(eventoRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo("ERROR");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when config not found")
        void shouldThrowWhenConfigNotFound() {
            SiatEventoRequest request = new SiatEventoRequest(
                    configId, 1, "Error", LocalDateTime.now(), null, null, null);
            given(configRepository.findById(configId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> eventoService.registrar(request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(codesClient, never()).registrarEvento(any(), any(), any(), anyInt(), any(), anyInt(), any(), any(), any());
        }

        @Test
        @DisplayName("Should use config's sucursal/puntoVenta when request has null values")
        void shouldUseConfigSucursalWhenRequestNull() {
            SiatEventoRequest request = new SiatEventoRequest(
                    configId, 3, "Falla sistema",
                    LocalDateTime.now().minusMinutes(30), null,
                    null, // null sucursal — should fall back to config's
                    null  // null puntoVenta
            );

            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true).datos(Map.of()).codigoRecepcion("REC-002").mensaje("OK").build();
            SiatEvento savedEvento = buildEvento(UUID.randomUUID(), 3);

            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCuisVigente(configId, 0, null)).willReturn(cuis);
            given(codesService.getCufdVigente(configId, 0, null)).willReturn(cufd);
            given(codesClient.registrarEvento(anyString(), anyString(), anyString(),
                    eq(0), any(), anyInt(), anyString(), anyString(), any())).willReturn(soapResp);
            given(eventoRepository.save(any(SiatEvento.class))).willReturn(savedEvento);
            given(eventoMapper.toResponse(savedEvento)).willReturn(mockEventoResponse);

            eventoService.registrar(request);

            // verify sucursal=0 (from config) was used in SOAP call
            verify(codesClient).registrarEvento(anyString(), anyString(), anyString(),
                    eq(0), any(), anyInt(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should use request's sucursal/puntoVenta when provided")
        void shouldUseRequestSucursalWhenProvided() {
            SiatEventoRequest request = new SiatEventoRequest(
                    configId, 1, "Evento sucursal 2",
                    LocalDateTime.now().minusMinutes(10), null,
                    2, // explicit sucursal
                    1  // explicit puntoVenta
            );

            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true).datos(Map.of()).codigoRecepcion("REC-003").mensaje("OK").build();
            SiatEvento savedEvento = buildEvento(UUID.randomUUID(), 1);

            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCuisVigente(configId, 2, 1)).willReturn(cuis);
            given(codesService.getCufdVigente(configId, 2, 1)).willReturn(cufd);
            given(codesClient.registrarEvento(anyString(), anyString(), anyString(),
                    eq(2), eq(1), anyInt(), anyString(), anyString(), any())).willReturn(soapResp);
            given(eventoRepository.save(any(SiatEvento.class))).willReturn(savedEvento);
            given(eventoMapper.toResponse(savedEvento)).willReturn(mockEventoResponse);

            eventoService.registrar(request);

            verify(codesClient).registrarEvento(anyString(), anyString(), anyString(),
                    eq(2), eq(1), anyInt(), anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should pass codigoEvento to saved entity")
        void shouldPersistCorrectCodigoEvento() {
            SiatEventoRequest request = new SiatEventoRequest(
                    configId, 4, "Evento clima",
                    LocalDateTime.now().minusMinutes(5), null, null, null);

            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true).datos(Map.of()).codigoRecepcion("REC-004").mensaje("OK").build();
            SiatEvento savedEvento = buildEvento(UUID.randomUUID(), 4);

            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCuisVigente(configId, 0, null)).willReturn(cuis);
            given(codesService.getCufdVigente(configId, 0, null)).willReturn(cufd);
            given(codesClient.registrarEvento(anyString(), anyString(), anyString(),
                    anyInt(), any(), anyInt(), anyString(), anyString(), any())).willReturn(soapResp);
            given(eventoRepository.save(any(SiatEvento.class))).willReturn(savedEvento);
            given(eventoMapper.toResponse(savedEvento)).willReturn(mockEventoResponse);

            eventoService.registrar(request);

            ArgumentCaptor<SiatEvento> captor = ArgumentCaptor.forClass(SiatEvento.class);
            verify(eventoRepository).save(captor.capture());
            assertThat(captor.getValue().getCodigoEvento()).isEqualTo(4);
        }
    }
}
