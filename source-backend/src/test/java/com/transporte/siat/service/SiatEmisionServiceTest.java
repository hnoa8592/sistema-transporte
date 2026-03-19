package com.transporte.siat.service;

import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.siat.client.SiatEmisionClient;
import com.transporte.siat.client.SiatSoapResponse;
import com.transporte.siat.dto.*;
import com.transporte.siat.entity.*;
import com.transporte.siat.enums.SiatEstadoEmision;
import com.transporte.siat.mapper.SiatEmisionMapper;
import com.transporte.siat.repository.*;
import com.transporte.siat.xml.SiatCufGenerator;
import com.transporte.siat.xml.SiatXmlBuilder;
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
import org.springframework.data.domain.Pageable;

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
@DisplayName("SiatEmisionService Unit Tests")
class SiatEmisionServiceTest {

    @Mock private SiatEmisionClient emisionClient;
    @Mock private SiatCodesService codesService;
    @Mock private SiatFacturaRepository facturaRepository;
    @Mock private SiatPaqueteRepository paqueteRepository;
    @Mock private SiatConfigRepository configRepository;
    @Mock private SiatCufdRepository cufdRepository;
    @Mock private SiatEmisionMapper emisionMapper;
    @Mock private SiatCufGenerator cufGenerator;
    @Mock private SiatXmlBuilder xmlBuilder;

    @InjectMocks
    private SiatEmisionService emisionService;

    private UUID configId;
    private SiatConfig config;
    private SiatCuis cuis;
    private SiatCufd cufd;
    private SiatFactura facturaValida;
    private SiatEmisionResponse mockEmisionResponse;

    @BeforeEach
    void setUp() {
        configId = UUID.randomUUID();
        config = buildConfig(configId);
        cuis = buildCuis(UUID.randomUUID(), config);
        cufd = buildCufd(UUID.randomUUID(), config);
        facturaValida = buildFactura(UUID.randomUUID(), SiatEstadoEmision.VALIDO, "CUF-VALID-001");
        mockEmisionResponse = buildEmisionResponse(facturaValida.getId(), SiatEstadoEmision.VALIDO);
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
        cufd.setCufd("CUFD-ABCDEFGHIJ1234567890");
        cufd.setCodigoControl("CTRL123");
        cufd.setCodigoParaQr("QR-DATA");
        cufd.setFechaVigencia(LocalDateTime.now().plusHours(23));
        cufd.setCodigoSucursal(0);
        cufd.setActivo(true);
        return cufd;
    }

    private SiatFactura buildFactura(UUID id, SiatEstadoEmision estado, String cufCode) {
        SiatFactura f = new SiatFactura();
        try {
            var field = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(f, id);
        } catch (Exception e) { /* ignore */ }
        f.setSiatConfig(config);
        f.setSiatCufd(cufd);
        f.setCuf(cufCode);
        f.setNumeroFactura(1L);
        f.setEstadoEmision(estado);
        f.setNitEmisor("1020304050");
        f.setRazonSocialEmisor("Transporte SRL");
        f.setNombreRazonSocial("Juan Pérez");
        f.setNumeroDocumento("12345678");
        f.setCodigoTipoDocumentoIdentidad(1);
        f.setImporteTotal(new BigDecimal("150.00"));
        f.setImporteTotalSujetoIva(new BigDecimal("150.00"));
        f.setFechaEmision(LocalDateTime.now().minusHours(1));
        f.setTipoEmision(1);
        f.setCodigoSucursal(0);
        f.setCodigoDocumentoSector(1);
        f.setCodigoActividad("461000");
        f.setDetalles(List.of());
        return f;
    }

    private SiatEmisionResponse buildEmisionResponse(UUID id, SiatEstadoEmision estado) {
        return new SiatEmisionResponse(id, null, "CUF-VALID-001", 1L,
                "1020304050", "Transporte SRL", "Juan Pérez",
                1, "12345678", new BigDecimal("150.00"), new BigDecimal("150.00"),
                BigDecimal.ONE, 1, 1, "461000", estado, 1,
                "REC-001", "OK", "QR-DATA",
                LocalDateTime.now(), List.of(), LocalDateTime.now());
    }

    private SiatEmisionRequest buildEmisionRequest(UUID cfgId, int tipoEmision) {
        SiatEmisionDetalleRequest detalle = new SiatEmisionDetalleRequest(
                "461000", 84111, "PROD-001", "Pasaje La Paz - Cochabamba",
                new BigDecimal("1"), 58, new BigDecimal("150.00"), BigDecimal.ZERO
        );
        return new SiatEmisionRequest(
                null, cfgId, "Juan Pérez", 1, "12345678",
                null, null, 1, 1, BigDecimal.ONE, tipoEmision, List.of(detalle)
        );
    }

    // ==================== CONSULTAS ====================

    @Nested
    @DisplayName("findById() tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return factura when found")
        void shouldReturnFacturaWhenFound() {
            UUID id = facturaValida.getId();
            given(facturaRepository.findById(id)).willReturn(Optional.of(facturaValida));
            given(emisionMapper.toResponse(facturaValida)).willReturn(mockEmisionResponse);

            SiatEmisionResponse result = emisionService.findById(id);

            assertThat(result).isNotNull();
            assertThat(result.estadoEmision()).isEqualTo(SiatEstadoEmision.VALIDO);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when factura not found")
        void shouldThrowWhenNotFound() {
            UUID id = UUID.randomUUID();
            given(facturaRepository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> emisionService.findById(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findByCuf() tests")
    class FindByCufTests {

        @Test
        @DisplayName("Should return factura when found by CUF")
        void shouldReturnFacturaByCuf() {
            given(facturaRepository.findByCuf("CUF-VALID-001")).willReturn(Optional.of(facturaValida));
            given(emisionMapper.toResponse(facturaValida)).willReturn(mockEmisionResponse);

            SiatEmisionResponse result = emisionService.findByCuf("CUF-VALID-001");

            assertThat(result).isNotNull();
            assertThat(result.cuf()).isEqualTo("CUF-VALID-001");
        }

        @Test
        @DisplayName("Should throw BusinessException when CUF not found")
        void shouldThrowWhenCufNotFound() {
            given(facturaRepository.findByCuf("INVALID-CUF")).willReturn(Optional.empty());

            assertThatThrownBy(() -> emisionService.findByCuf("INVALID-CUF"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No se encontró factura con CUF");
        }
    }

    // ==================== EMISIÓN INDIVIDUAL ====================

    @Nested
    @DisplayName("emitirFactura() tests")
    class EmitirFacturaTests {

        @Test
        @DisplayName("Should emit online factura and set estado VALIDO when SIN accepts")
        void shouldEmitOnlineFacturaValidoWhenSinAccepts() {
            SiatEmisionRequest request = buildEmisionRequest(configId, 1);
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true).datos(Map.of()).codigoRecepcion("REC-001").mensaje("OK").build();

            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCufdVigente(configId, 0, null)).willReturn(cufd);
            given(facturaRepository.findMaxNumeroFactura(configId, 0)).willReturn(0L);
            given(facturaRepository.save(any(SiatFactura.class))).willReturn(facturaValida);
            given(cufGenerator.generate(anyString(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyLong(), anyInt(), anyString()))
                    .willReturn("CUF-GENERATED-ABC123");
            given(xmlBuilder.buildFacturaXml(any())).willReturn("<factura>XML</factura>");
            given(cufGenerator.generateQrContent(anyString(), any(), any(), anyLong(), anyString(), anyString(), any(), any()))
                    .willReturn("QR-CONTENT");
            given(codesService.getCuisVigente(configId, 0, null)).willReturn(cuis);
            given(emisionClient.recepcionFactura(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), anyString(), anyString(), anyString(), anyInt())).willReturn(soapResp);
            given(emisionMapper.toResponse(any(SiatFactura.class))).willReturn(mockEmisionResponse);

            SiatEmisionResponse result = emisionService.emitirFactura(request);

            assertThat(result).isNotNull();
            // Verify CUF was generated
            verify(cufGenerator).generate(anyString(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyLong(), anyInt(), anyString());
            // Verify XML was built
            verify(xmlBuilder).buildFacturaXml(any());
            // Verify SOAP call was made
            verify(emisionClient).recepcionFactura(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), anyString(), anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("Should set estado OBSERVADO when SIN does not accept online emission")
        void shouldSetEstadoObservadoWhenSinRejects() {
            SiatEmisionRequest request = buildEmisionRequest(configId, 1);
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(false).datos(Map.of()).mensaje("Factura observada").build();

            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCufdVigente(configId, 0, null)).willReturn(cufd);
            given(facturaRepository.findMaxNumeroFactura(configId, 0)).willReturn(5L);
            given(facturaRepository.save(any(SiatFactura.class))).willReturn(facturaValida);
            given(cufGenerator.generate(anyString(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyLong(), anyInt(), anyString()))
                    .willReturn("CUF-OBS-001");
            given(xmlBuilder.buildFacturaXml(any())).willReturn("<factura/>");
            given(cufGenerator.generateQrContent(anyString(), any(), any(), anyLong(), anyString(), anyString(), any(), any()))
                    .willReturn("QR");
            given(codesService.getCuisVigente(configId, 0, null)).willReturn(cuis);
            given(emisionClient.recepcionFactura(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), anyString(), anyString(), anyString(), anyInt())).willReturn(soapResp);

            SiatEmisionResponse observadoResp = buildEmisionResponse(facturaValida.getId(), SiatEstadoEmision.OBSERVADO);
            given(emisionMapper.toResponse(any(SiatFactura.class))).willReturn(observadoResp);

            SiatEmisionResponse result = emisionService.emitirFactura(request);

            // Estado should be OBSERVADO after failed SIN response
            ArgumentCaptor<SiatFactura> captor = ArgumentCaptor.forClass(SiatFactura.class);
            verify(facturaRepository, atLeast(2)).save(captor.capture());
            List<SiatFactura> savedFacturas = captor.getAllValues();
            SiatFactura lastSaved = savedFacturas.get(savedFacturas.size() - 1);
            assertThat(lastSaved.getEstadoEmision()).isEqualTo(SiatEstadoEmision.OBSERVADO);
        }

        @Test
        @DisplayName("Should set estado EN_PAQUETE for offline (tipoEmision=2) without SOAP call")
        void shouldSetEstadoEnPaqueteForOfflineEmision() {
            SiatEmisionRequest request = buildEmisionRequest(configId, 2);

            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCufdVigente(configId, 0, null)).willReturn(cufd);
            given(facturaRepository.findMaxNumeroFactura(configId, 0)).willReturn(0L);
            given(facturaRepository.save(any(SiatFactura.class))).willReturn(facturaValida);
            given(cufGenerator.generate(anyString(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyLong(), anyInt(), anyString()))
                    .willReturn("CUF-OFFLINE-001");
            given(xmlBuilder.buildFacturaXml(any())).willReturn("<factura/>");
            given(cufGenerator.generateQrContent(anyString(), any(), any(), anyLong(), anyString(), anyString(), any(), any()))
                    .willReturn("QR");
            SiatEmisionResponse offlineResp = buildEmisionResponse(facturaValida.getId(), SiatEstadoEmision.EN_PAQUETE);
            given(emisionMapper.toResponse(any())).willReturn(offlineResp);

            SiatEmisionResponse result = emisionService.emitirFactura(request);

            // For offline, no SOAP call should be made
            verify(emisionClient, never()).recepcionFactura(any(), any(), any(), any(),
                    anyInt(), any(), any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when config not found")
        void shouldThrowWhenConfigNotFound() {
            SiatEmisionRequest request = buildEmisionRequest(configId, 1);
            given(configRepository.findById(configId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> emisionService.emitirFactura(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw BusinessException when no vigente CUFD")
        void shouldThrowWhenNoCufd() {
            SiatEmisionRequest request = buildEmisionRequest(configId, 1);
            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCufdVigente(configId, 0, null))
                    .willThrow(new BusinessException("No existe CUFD vigente"));

            assertThatThrownBy(() -> emisionService.emitirFactura(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No existe CUFD vigente");
        }

        @Test
        @DisplayName("Should calculate total from detail lines")
        void shouldCalculateTotalFromDetails() {
            SiatEmisionDetalleRequest det1 = new SiatEmisionDetalleRequest(
                    "461000", 84111, null, "Pasaje",
                    new BigDecimal("2"), 58, new BigDecimal("100.00"), BigDecimal.ZERO);
            SiatEmisionDetalleRequest det2 = new SiatEmisionDetalleRequest(
                    "461000", 84111, null, "Encomienda",
                    new BigDecimal("1"), 58, new BigDecimal("50.00"), BigDecimal.ZERO);
            SiatEmisionRequest request = new SiatEmisionRequest(
                    null, configId, "Ana García", 1, "87654321",
                    null, null, 1, 1, BigDecimal.ONE, 2, List.of(det1, det2)
            );

            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCufdVigente(configId, 0, null)).willReturn(cufd);
            given(facturaRepository.findMaxNumeroFactura(configId, 0)).willReturn(0L);
            given(facturaRepository.save(any(SiatFactura.class))).willReturn(facturaValida);
            given(cufGenerator.generate(anyString(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyLong(), anyInt(), anyString()))
                    .willReturn("CUF-CALC-001");
            given(xmlBuilder.buildFacturaXml(any())).willReturn("<f/>");
            given(cufGenerator.generateQrContent(anyString(), any(), any(), anyLong(), anyString(), anyString(), any(), any()))
                    .willReturn("QR");
            given(emisionMapper.toResponse(any())).willReturn(mockEmisionResponse);

            emisionService.emitirFactura(request);

            // First save sets up the entity with totals
            ArgumentCaptor<SiatFactura> captor = ArgumentCaptor.forClass(SiatFactura.class);
            verify(facturaRepository, atLeast(1)).save(captor.capture());
            SiatFactura firstSave = captor.getAllValues().get(0);
            // 2 * 100 + 1 * 50 = 250
            assertThat(firstSave.getImporteTotal()).isEqualByComparingTo(new BigDecimal("250.00"));
        }

        @Test
        @DisplayName("Should set factura numero based on max + 1")
        void shouldSetNumeroFacturaBasedOnMax() {
            SiatEmisionRequest request = buildEmisionRequest(configId, 2);

            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCufdVigente(configId, 0, null)).willReturn(cufd);
            given(facturaRepository.findMaxNumeroFactura(configId, 0)).willReturn(42L);
            given(facturaRepository.save(any(SiatFactura.class))).willReturn(facturaValida);
            given(cufGenerator.generate(anyString(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyLong(), anyInt(), anyString()))
                    .willReturn("CUF-NUM-001");
            given(xmlBuilder.buildFacturaXml(any())).willReturn("<f/>");
            given(cufGenerator.generateQrContent(anyString(), any(), any(), anyLong(), anyString(), anyString(), any(), any()))
                    .willReturn("QR");
            given(emisionMapper.toResponse(any())).willReturn(mockEmisionResponse);

            emisionService.emitirFactura(request);

            ArgumentCaptor<SiatFactura> captor = ArgumentCaptor.forClass(SiatFactura.class);
            verify(facturaRepository, atLeast(1)).save(captor.capture());
            SiatFactura first = captor.getAllValues().get(0);
            assertThat(first.getNumeroFactura()).isEqualTo(43L); // 42 + 1
        }
    }

    // ==================== PAQUETE OFFLINE ====================

    @Nested
    @DisplayName("emitirPaquete() tests")
    class EmitirPaqueteTests {

        @Test
        @DisplayName("Should throw BusinessException when no EN_PAQUETE facturas exist")
        void shouldThrowWhenNoFacturasPendientes() {
            Page<SiatFactura> emptyPage = new PageImpl<>(List.of());
            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCufdVigente(configId, 0, null)).willReturn(cufd);
            given(facturaRepository.findByEstadoEmision(SiatEstadoEmision.EN_PAQUETE, Pageable.unpaged()))
                    .willReturn(emptyPage);

            assertThatThrownBy(() -> emisionService.emitirPaquete(configId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No hay facturas pendientes de paquete");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when config not found for paquete")
        void shouldThrowWhenConfigNotFoundForPaquete() {
            given(configRepository.findById(configId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> emisionService.emitirPaquete(configId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== MASIVA ====================

    @Nested
    @DisplayName("emitirMasiva() tests")
    class EmitirMasivaTests {

        @Test
        @DisplayName("Should throw BusinessException when no PENDIENTE facturas exist")
        void shouldThrowWhenNoFacturasPendientesForMasiva() {
            Page<SiatFactura> emptyPage = new PageImpl<>(List.of());
            given(configRepository.findById(configId)).willReturn(Optional.of(config));
            given(codesService.getCufdVigente(configId, 0, null)).willReturn(cufd);
            given(facturaRepository.findByEstadoEmision(SiatEstadoEmision.PENDIENTE, Pageable.unpaged()))
                    .willReturn(emptyPage);

            assertThatThrownBy(() -> emisionService.emitirMasiva(configId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No hay facturas pendientes para emisión masiva");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when config not found for masiva")
        void shouldThrowWhenConfigNotFoundForMasiva() {
            given(configRepository.findById(configId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> emisionService.emitirMasiva(configId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== VERIFICAR ESTADO ====================

    @Nested
    @DisplayName("verificarEstado() tests")
    class VerificarEstadoTests {

        @Test
        @DisplayName("Should update factura to VALIDO when SIN confirms it")
        void shouldUpdateToValidoWhenSinConfirms() {
            UUID id = facturaValida.getId();
            SiatSoapResponse soapResp = SiatSoapResponse.builder()
                    .exitoso(true).datos(Map.of()).mensaje("Factura válida").build();

            given(facturaRepository.findById(id)).willReturn(Optional.of(facturaValida));
            given(codesService.getCuisVigente(configId, 0, null)).willReturn(cuis);
            given(emisionClient.verificarEstado(anyString(), anyString(), anyString(), anyString(),
                    anyInt(), any(), anyString())).willReturn(soapResp);
            given(facturaRepository.save(any(SiatFactura.class))).willReturn(facturaValida);
            given(emisionMapper.toResponse(facturaValida)).willReturn(mockEmisionResponse);

            SiatEmisionResponse result = emisionService.verificarEstado(id);

            assertThat(result).isNotNull();
            ArgumentCaptor<SiatFactura> captor = ArgumentCaptor.forClass(SiatFactura.class);
            verify(facturaRepository).save(captor.capture());
            assertThat(captor.getValue().getEstadoEmision()).isEqualTo(SiatEstadoEmision.VALIDO);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when factura not found for verificar")
        void shouldThrowWhenNotFoundForVerificar() {
            UUID id = UUID.randomUUID();
            given(facturaRepository.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> emisionService.verificarEstado(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
