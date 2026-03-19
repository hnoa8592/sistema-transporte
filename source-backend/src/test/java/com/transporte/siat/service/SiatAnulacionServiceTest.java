package com.transporte.siat.service;

import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.siat.client.SiatEmisionClient;
import com.transporte.siat.client.SiatSoapResponse;
import com.transporte.siat.dto.SiatAnulacionRequest;
import com.transporte.siat.dto.SiatEmisionResponse;
import com.transporte.siat.dto.SiatReversionRequest;
import com.transporte.siat.entity.SiatConfig;
import com.transporte.siat.entity.SiatCufd;
import com.transporte.siat.entity.SiatCuis;
import com.transporte.siat.entity.SiatFactura;
import com.transporte.siat.enums.SiatEstadoEmision;
import com.transporte.siat.mapper.SiatEmisionMapper;
import com.transporte.siat.repository.SiatFacturaRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiatAnulacionService Unit Tests")
class SiatAnulacionServiceTest {

    @Mock private SiatEmisionClient emisionClient;
    @Mock private SiatCodesService codesService;
    @Mock private SiatFacturaRepository facturaRepository;
    @Mock private SiatEmisionMapper emisionMapper;

    @InjectMocks
    private SiatAnulacionService anulacionService;

    private UUID facturaId;
    private SiatFactura facturaValida;
    private SiatFactura facturaAnulada;
    private SiatConfig config;
    private SiatCufd cufd;
    private SiatCuis cuis;
    private SiatEmisionResponse mockResponse;

    @BeforeEach
    void setUp() {
        facturaId = UUID.randomUUID();
        UUID configId = UUID.randomUUID();
        config = buildConfig(configId);
        cuis = buildCuis(UUID.randomUUID(), config);
        cufd = buildCufd(UUID.randomUUID(), config);
        facturaValida = buildFactura(facturaId, SiatEstadoEmision.VALIDO, "CUF-ABC123", config, cufd);
        facturaAnulada = buildFactura(UUID.randomUUID(), SiatEstadoEmision.ANULADO, "CUF-XYZ789", config, cufd);
        mockResponse = buildResponse(facturaId, SiatEstadoEmision.ANULADO);
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
        c.setCodigoModalidad(2);
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

    private SiatFactura buildFactura(UUID id, SiatEstadoEmision estado, String cufCode,
                                      SiatConfig cfg, SiatCufd cufdRef) {
        SiatFactura f = new SiatFactura();
        try {
            var field = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(f, id);
        } catch (Exception e) { /* ignore */ }
        f.setSiatConfig(cfg);
        f.setSiatCufd(cufdRef);
        f.setCuf(cufCode);
        f.setNumeroFactura(1L);
        f.setEstadoEmision(estado);
        f.setNitEmisor("1020304050");
        f.setRazonSocialEmisor("Transporte SRL");
        f.setNombreRazonSocial("Juan Pérez");
        f.setNumeroDocumento("12345678");
        f.setImporteTotal(new BigDecimal("150.00"));
        f.setImporteTotalSujetoIva(new BigDecimal("150.00"));
        f.setFechaEmision(LocalDateTime.now().minusHours(1));
        f.setTipoEmision(1);
        f.setDetalles(List.of());
        return f;
    }

    private SiatEmisionResponse buildResponse(UUID id, SiatEstadoEmision estado) {
        return new SiatEmisionResponse(id, null, "CUF-ABC123", 1L,
                "1020304050", "Transporte SRL", "Juan Pérez",
                1, "12345678", new BigDecimal("150.00"), new BigDecimal("150.00"),
                BigDecimal.ONE, 1, 1, "461000", estado, 1, null, null, null,
                LocalDateTime.now(), List.of(), LocalDateTime.now());
    }

    // ==================== ANULACIÓN ====================

    @Nested
    @DisplayName("anular() tests")
    class AnularTests {

        @Test
        @DisplayName("Should anular factura VALIDO successfully")
        void shouldAnularFacturaValida() {
            SiatAnulacionRequest request = new SiatAnulacionRequest(facturaId, 1);
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true).datos(Map.of()).mensaje("Anulación aceptada").build();

            given(facturaRepository.findById(facturaId)).willReturn(Optional.of(facturaValida));
            given(codesService.getCuisVigente(any(), anyInt(), any())).willReturn(cuis);
            given(emisionClient.anulacionFactura(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), anyString(), anyInt())).willReturn(soapResp);
            given(facturaRepository.save(any(SiatFactura.class))).willReturn(facturaValida);
            given(emisionMapper.toResponse(facturaValida)).willReturn(mockResponse);

            SiatEmisionResponse result = anulacionService.anular(request);

            assertThat(result).isNotNull();
            ArgumentCaptor<SiatFactura> captor = ArgumentCaptor.forClass(SiatFactura.class);
            verify(facturaRepository).save(captor.capture());
            assertThat(captor.getValue().getEstadoEmision()).isEqualTo(SiatEstadoEmision.ANULADO);
        }

        @Test
        @DisplayName("Should throw BusinessException when factura is already ANULADO")
        void shouldThrowWhenAlreadyAnulado() {
            SiatAnulacionRequest request = new SiatAnulacionRequest(facturaAnulada.getId(), 1);
            given(facturaRepository.findById(facturaAnulada.getId())).willReturn(Optional.of(facturaAnulada));

            assertThatThrownBy(() -> anulacionService.anular(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya está anulada");

            verify(emisionClient, never()).anulacionFactura(any(), any(), any(), any(), anyInt(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("Should throw BusinessException when factura has no CUF")
        void shouldThrowWhenNoCuf() {
            facturaValida.setCuf(null);
            SiatAnulacionRequest request = new SiatAnulacionRequest(facturaId, 1);
            given(facturaRepository.findById(facturaId)).willReturn(Optional.of(facturaValida));

            assertThatThrownBy(() -> anulacionService.anular(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no tiene CUF");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when factura not found")
        void shouldThrowWhenNotFound() {
            SiatAnulacionRequest request = new SiatAnulacionRequest(facturaId, 1);
            given(facturaRepository.findById(facturaId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> anulacionService.anular(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw BusinessException when SIN rejects anulación")
        void shouldThrowWhenSinRejectsAnulacion() {
            SiatAnulacionRequest request = new SiatAnulacionRequest(facturaId, 1);
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(false).datos(Map.of()).mensaje("Factura no anulable").build();

            given(facturaRepository.findById(facturaId)).willReturn(Optional.of(facturaValida));
            given(codesService.getCuisVigente(any(), anyInt(), any())).willReturn(cuis);
            given(emisionClient.anulacionFactura(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), anyString(), anyInt())).willReturn(soapResp);

            assertThatThrownBy(() -> anulacionService.anular(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("SIN rechazó la anulación");

            // Should not persist when rejected
            verify(facturaRepository, never()).save(any(SiatFactura.class));
        }

        @Test
        @DisplayName("Should pass correct codigoMotivo to SIN client")
        void shouldPassCodigoMotivoToClient() {
            SiatAnulacionRequest request = new SiatAnulacionRequest(facturaId, 5);
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true).datos(Map.of()).mensaje("OK").build();

            given(facturaRepository.findById(facturaId)).willReturn(Optional.of(facturaValida));
            given(codesService.getCuisVigente(any(), anyInt(), any())).willReturn(cuis);
            given(emisionClient.anulacionFactura(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), anyString(), anyInt())).willReturn(soapResp);
            given(facturaRepository.save(any())).willReturn(facturaValida);
            given(emisionMapper.toResponse(any())).willReturn(mockResponse);

            anulacionService.anular(request);

            verify(emisionClient).anulacionFactura(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), eq("CUF-ABC123"), eq(5));
        }
    }

    // ==================== REVERSIÓN ====================

    @Nested
    @DisplayName("revertir() tests")
    class RevertirTests {

        @Test
        @DisplayName("Should revertir factura ANULADO successfully")
        void shouldRevertirFacturaAnulada() {
            SiatReversionRequest request = new SiatReversionRequest(facturaAnulada.getId());
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true).datos(Map.of()).mensaje("Reversión aceptada").build();
            SiatEmisionResponse revResponse = buildResponse(facturaAnulada.getId(), SiatEstadoEmision.REVERTIDO);

            given(facturaRepository.findById(facturaAnulada.getId())).willReturn(Optional.of(facturaAnulada));
            given(codesService.getCuisVigente(any(), anyInt(), any())).willReturn(cuis);
            given(emisionClient.reversionAnulacion(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), anyString())).willReturn(soapResp);
            given(facturaRepository.save(any(SiatFactura.class))).willReturn(facturaAnulada);
            given(emisionMapper.toResponse(facturaAnulada)).willReturn(revResponse);

            SiatEmisionResponse result = anulacionService.revertir(request);

            assertThat(result).isNotNull();
            ArgumentCaptor<SiatFactura> captor = ArgumentCaptor.forClass(SiatFactura.class);
            verify(facturaRepository).save(captor.capture());
            assertThat(captor.getValue().getEstadoEmision()).isEqualTo(SiatEstadoEmision.REVERTIDO);
        }

        @Test
        @DisplayName("Should throw BusinessException when reversing non-ANULADO factura")
        void shouldThrowWhenNotAnulado() {
            SiatReversionRequest request = new SiatReversionRequest(facturaId);
            given(facturaRepository.findById(facturaId)).willReturn(Optional.of(facturaValida));

            assertThatThrownBy(() -> anulacionService.revertir(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo se pueden revertir facturas con estado ANULADO");

            verify(emisionClient, never()).reversionAnulacion(any(), any(), any(), any(), anyInt(), any(), any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when factura not found for reversion")
        void shouldThrowWhenNotFound() {
            UUID missingId = UUID.randomUUID();
            SiatReversionRequest request = new SiatReversionRequest(missingId);
            given(facturaRepository.findById(missingId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> anulacionService.revertir(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw BusinessException when SIN rejects reversion")
        void shouldThrowWhenSinRejectsReversion() {
            SiatReversionRequest request = new SiatReversionRequest(facturaAnulada.getId());
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(false).datos(Map.of()).mensaje("Reversión no permitida").build();

            given(facturaRepository.findById(facturaAnulada.getId())).willReturn(Optional.of(facturaAnulada));
            given(codesService.getCuisVigente(any(), anyInt(), any())).willReturn(cuis);
            given(emisionClient.reversionAnulacion(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), anyString())).willReturn(soapResp);

            assertThatThrownBy(() -> anulacionService.revertir(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("SIN rechazó la reversión");

            verify(facturaRepository, never()).save(any(SiatFactura.class));
        }

        @Test
        @DisplayName("Should pass CUF to SIN client for reversion")
        void shouldPassCufToClientForReversion() {
            SiatReversionRequest request = new SiatReversionRequest(facturaAnulada.getId());
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true).datos(Map.of()).mensaje("OK").build();

            given(facturaRepository.findById(facturaAnulada.getId())).willReturn(Optional.of(facturaAnulada));
            given(codesService.getCuisVigente(any(), anyInt(), any())).willReturn(cuis);
            given(emisionClient.reversionAnulacion(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), anyString())).willReturn(soapResp);
            given(facturaRepository.save(any())).willReturn(facturaAnulada);
            given(emisionMapper.toResponse(any())).willReturn(buildResponse(facturaAnulada.getId(), SiatEstadoEmision.REVERTIDO));

            anulacionService.revertir(request);

            verify(emisionClient).reversionAnulacion(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), eq("CUF-XYZ789"));
        }
    }
}
