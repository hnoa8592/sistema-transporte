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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio de emisión de facturas SIAT Bolivia.
 * Cubre emisión individual, en paquete y masiva.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiatEmisionService {

    private final SiatEmisionClient emisionClient;
    private final SiatCodesService codesService;
    private final SiatFacturaRepository facturaRepository;
    private final SiatPaqueteRepository paqueteRepository;
    private final SiatConfigRepository configRepository;
    private final SiatCufdRepository cufdRepository;
    private final SiatEmisionMapper emisionMapper;
    private final SiatCufGenerator cufGenerator;
    private final SiatXmlBuilder xmlBuilder;

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    // ==================== CONSULTAS ====================

    public SiatEmisionResponse findById(UUID id) {
        return emisionMapper.toResponse(facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SiatFactura", id)));
    }

    public SiatEmisionResponse findByInvoiceId(UUID invoiceId) {
        return emisionMapper.toResponse(facturaRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("SiatFactura para invoice", invoiceId)));
    }

    public SiatEmisionResponse findByCuf(String cuf) {
        return emisionMapper.toResponse(facturaRepository.findByCuf(cuf)
                .orElseThrow(() -> new BusinessException("No se encontró factura con CUF: " + cuf)));
    }

    // ==================== EMISIÓN INDIVIDUAL ====================

    /**
     * Emite una factura de forma individual (modo online).
     * Flujo: generar CUF → construir XML → enviar al SIN.
     */
    @Transactional
    public SiatEmisionResponse emitirFactura(SiatEmisionRequest request) {
        SiatConfig config = configRepository.findById(request.siatConfigId())
                .orElseThrow(() -> new ResourceNotFoundException("SiatConfig", request.siatConfigId()));

        SiatCufd cufd = codesService.getCufdVigente(
                config.getId(), config.getCodigoSucursal(), config.getCodigoPuntoVenta());

        SiatFactura factura = buildFactura(request, config, cufd,
                request.tipoEmision() != null ? request.tipoEmision() : 1);

        // Persistir primero para obtener el numero de factura (sequence)
        facturaRepository.save(factura);

        // Generar CUF y QR
        String cuf = cufGenerator.generate(
                config.getNit(), factura.getFechaEmision(),
                config.getCodigoSucursal(), config.getCodigoModalidad(),
                factura.getCodigoDocumentoSector(), factura.getTipoEmision(),
                factura.getNumeroFactura(),
                config.getCodigoPuntoVenta() != null ? config.getCodigoPuntoVenta() : 0,
                cufd.getCufd());
        factura.setCuf(cuf);

        String xml = xmlBuilder.buildFacturaXml(factura);
        factura.setXmlContent(xml);

        String qr = cufGenerator.generateQrContent(config.getNit(), factura.getFechaEmision(),
                factura.getImporteTotal(), factura.getNumeroFactura(),
                cufd.getCufd(), cuf, null, cufd.getCodigoControl());
        factura.setQrContent(qr);

        // Enviar al SIN si es emisión online
        if (factura.getTipoEmision() == 1) {
            SiatSoapResponse siatResp = emisionClient.recepcionFactura(
                    config.getNit(), config.getCodigoSistema(),
                    codesService.getCuisVigente(config.getId(),
                            config.getCodigoSucursal(), config.getCodigoPuntoVenta()).getCuis(),
                    cufd.getCufd(),
                    config.getCodigoSucursal(), config.getCodigoPuntoVenta(),
                    xml, cuf, factura.getFechaEmision().format(ISO_FMT), 1);

            factura.setCodigoRecepcion(siatResp.getCodigoRecepcion());
            factura.setMensajeSiat(siatResp.getMensaje());
            factura.setEstadoEmision(siatResp.isExitoso()
                    ? SiatEstadoEmision.VALIDO : SiatEstadoEmision.OBSERVADO);
        }

        return emisionMapper.toResponse(facturaRepository.save(factura));
    }

    // ==================== PAQUETE (OFFLINE) ====================

    /**
     * Agrupa facturas pendientes en un paquete ZIP y lo envía al SIN.
     */
    @Transactional
    public SiatPaqueteResponse emitirPaquete(UUID siatConfigId) {
        SiatConfig config = configRepository.findById(siatConfigId)
                .orElseThrow(() -> new ResourceNotFoundException("SiatConfig", siatConfigId));

        SiatCufd cufd = codesService.getCufdVigente(
                config.getId(), config.getCodigoSucursal(), config.getCodigoPuntoVenta());

        List<SiatFactura> pendientes = facturaRepository
                .findByEstadoEmision(SiatEstadoEmision.EN_PAQUETE, Pageable.unpaged())
                .stream()
                .filter(f -> f.getSiatConfig().getId().equals(siatConfigId))
                .toList();

        if (pendientes.isEmpty()) {
            throw new BusinessException("No hay facturas pendientes de paquete para esta configuración");
        }

        // Construir ZIP con los XMLs
        String zipB64 = buildZip(pendientes);

        SiatPaquete paquete = SiatPaquete.builder()
                .siatConfig(config)
                .siatCufd(cufd)
                .codigoSucursal(config.getCodigoSucursal())
                .codigoPuntoVenta(config.getCodigoPuntoVenta())
                .cantidadFacturas(pendientes.size())
                .tipoEmision(2)
                .estado("ENVIADO")
                .fechaEmision(LocalDateTime.now())
                .archivoZip(zipB64)
                .build();

        paquete = paqueteRepository.save(paquete);

        SiatSoapResponse siatResp = emisionClient.recepcionPaquete(
                config.getNit(), config.getCodigoSistema(),
                codesService.getCuisVigente(config.getId(),
                        config.getCodigoSucursal(), config.getCodigoPuntoVenta()).getCuis(),
                cufd.getCufd(),
                config.getCodigoSucursal(), config.getCodigoPuntoVenta(),
                zipB64, pendientes.size(),
                LocalDateTime.now().format(ISO_FMT), 2);

        paquete.setCodigoRecepcion(siatResp.getCodigoRecepcion());
        paquete.setMensajeSiat(siatResp.getMensaje());
        paquete.setEstado(siatResp.isExitoso() ? "ENVIADO" : "ERROR");

        // Asociar facturas al paquete
        final UUID paqueteId = paquete.getId();
        pendientes.forEach(f -> {
            f.setSiatPaqueteId(paqueteId);
            f.setEstadoEmision(siatResp.isExitoso()
                    ? SiatEstadoEmision.ENVIADO : SiatEstadoEmision.EN_PAQUETE);
            facturaRepository.save(f);
        });

        return toResponse(paqueteRepository.save(paquete));
    }

    /**
     * Consulta el estado de validación de un paquete previamente enviado.
     */
    @Transactional
    public SiatPaqueteResponse validarPaquete(UUID paqueteId) {
        SiatPaquete paquete = paqueteRepository.findById(paqueteId)
                .orElseThrow(() -> new ResourceNotFoundException("SiatPaquete", paqueteId));

        if (paquete.getCodigoRecepcion() == null) {
            throw new BusinessException("El paquete no tiene código de recepción del SIN");
        }

        SiatConfig config = paquete.getSiatConfig();
        SiatCufd cufd = paquete.getSiatCufd();

        SiatSoapResponse siatResp = emisionClient.validacionPaquete(
                config.getNit(), config.getCodigoSistema(),
                codesService.getCuisVigente(config.getId(),
                        config.getCodigoSucursal(), config.getCodigoPuntoVenta()).getCuis(),
                cufd.getCufd(),
                config.getCodigoSucursal(), config.getCodigoPuntoVenta(),
                paquete.getCodigoRecepcion());

        String estadoVal = siatResp.getDatos().getOrDefault("estadoValidacion", "DESCONOCIDO");
        paquete.setEstadoValidacion(estadoVal);
        paquete.setMensajeSiat(siatResp.getMensaje());

        if ("FINALIZADO".equalsIgnoreCase(estadoVal)) {
            paquete.setEstado("VALIDADO");
            // Actualizar facturas del paquete a VALIDO
            facturaRepository.findBySiatPaqueteId(paqueteId).forEach(f -> {
                f.setEstadoEmision(SiatEstadoEmision.VALIDO);
                facturaRepository.save(f);
            });
        }

        return toResponse(paqueteRepository.save(paquete));
    }

    // ==================== MASIVA ====================

    /**
     * Emite facturas en modalidad masiva (lote de facturas sin CUFD individual).
     */
    @Transactional
    public SiatPaqueteResponse emitirMasiva(UUID siatConfigId) {
        SiatConfig config = configRepository.findById(siatConfigId)
                .orElseThrow(() -> new ResourceNotFoundException("SiatConfig", siatConfigId));

        SiatCufd cufd = codesService.getCufdVigente(
                config.getId(), config.getCodigoSucursal(), config.getCodigoPuntoVenta());

        List<SiatFactura> pendientes = facturaRepository
                .findByEstadoEmision(SiatEstadoEmision.PENDIENTE, Pageable.unpaged())
                .stream()
                .filter(f -> f.getSiatConfig().getId().equals(siatConfigId))
                .toList();

        if (pendientes.isEmpty()) {
            throw new BusinessException("No hay facturas pendientes para emisión masiva");
        }

        String zipB64 = buildZip(pendientes);
        String fechaEnvio = LocalDateTime.now().format(ISO_FMT);

        SiatSoapResponse siatResp = emisionClient.recepcionMasiva(
                config.getNit(), config.getCodigoSistema(),
                codesService.getCuisVigente(config.getId(),
                        config.getCodigoSucursal(), config.getCodigoPuntoVenta()).getCuis(),
                cufd.getCufd(),
                config.getCodigoSucursal(), config.getCodigoPuntoVenta(),
                zipB64, pendientes.size(), fechaEnvio);

        SiatPaquete paquete = SiatPaquete.builder()
                .siatConfig(config)
                .siatCufd(cufd)
                .codigoSucursal(config.getCodigoSucursal())
                .codigoPuntoVenta(config.getCodigoPuntoVenta())
                .cantidadFacturas(pendientes.size())
                .tipoEmision(1) // masiva online
                .codigoRecepcion(siatResp.getCodigoRecepcion())
                .estado(siatResp.isExitoso() ? "ENVIADO" : "ERROR")
                .mensajeSiat(siatResp.getMensaje())
                .fechaEmision(LocalDateTime.now())
                .archivoZip(zipB64)
                .build();

        return toResponse(paqueteRepository.save(paquete));
    }

    @Transactional
    public SiatPaqueteResponse validarMasiva(UUID paqueteId) {
        SiatPaquete paquete = paqueteRepository.findById(paqueteId)
                .orElseThrow(() -> new ResourceNotFoundException("SiatPaquete", paqueteId));

        SiatConfig config = paquete.getSiatConfig();
        SiatCufd cufd = paquete.getSiatCufd();

        SiatSoapResponse siatResp = emisionClient.validacionMasiva(
                config.getNit(), config.getCodigoSistema(),
                codesService.getCuisVigente(config.getId(),
                        config.getCodigoSucursal(), config.getCodigoPuntoVenta()).getCuis(),
                cufd.getCufd(),
                config.getCodigoSucursal(), config.getCodigoPuntoVenta(),
                paquete.getCodigoRecepcion());

        paquete.setEstadoValidacion(siatResp.getDatos().getOrDefault("estadoValidacion", "DESCONOCIDO"));
        paquete.setMensajeSiat(siatResp.getMensaje());
        return toResponse(paqueteRepository.save(paquete));
    }

    // ==================== VERIFICACIÓN ESTADO ====================

    @Transactional
    public SiatEmisionResponse verificarEstado(UUID siatFacturaId) {
        SiatFactura factura = facturaRepository.findById(siatFacturaId)
                .orElseThrow(() -> new ResourceNotFoundException("SiatFactura", siatFacturaId));

        SiatConfig config = factura.getSiatConfig();
        SiatCufd cufd = factura.getSiatCufd();

        SiatSoapResponse siatResp = emisionClient.verificarEstado(
                config.getNit(), config.getCodigoSistema(),
                codesService.getCuisVigente(config.getId(),
                        config.getCodigoSucursal(), config.getCodigoPuntoVenta()).getCuis(),
                cufd.getCufd(),
                config.getCodigoSucursal(), config.getCodigoPuntoVenta(),
                factura.getCuf());

        factura.setMensajeSiat(siatResp.getMensaje());
        if (siatResp.isExitoso()) {
            factura.setEstadoEmision(SiatEstadoEmision.VALIDO);
        }
        return emisionMapper.toResponse(facturaRepository.save(factura));
    }

    // ==================== PRIVADO ====================

    private SiatFactura buildFactura(SiatEmisionRequest request, SiatConfig config,
                                      SiatCufd cufd, int tipoEmision) {
        Long nextNum = facturaRepository.findMaxNumeroFactura(config.getId(), config.getCodigoSucursal()) + 1;
        LocalDateTime ahora = LocalDateTime.now();

        SiatFactura factura = new SiatFactura();
        factura.setInvoiceId(request.invoiceId());
        factura.setSiatConfig(config);
        factura.setSiatCufd(cufd);
        factura.setNumeroFactura(nextNum);
        factura.setCodigoSucursal(config.getCodigoSucursal());
        factura.setCodigoPuntoVenta(config.getCodigoPuntoVenta());
        factura.setFechaEmision(ahora);
        factura.setNitEmisor(config.getNit());
        factura.setRazonSocialEmisor(config.getRazonSocial());
        factura.setNombreRazonSocial(request.nombreRazonSocial());
        factura.setCodigoTipoDocumentoIdentidad(
                request.codigoTipoDocumentoIdentidad() != null ? request.codigoTipoDocumentoIdentidad() : 1);
        factura.setNumeroDocumento(request.numeroDocumento());
        factura.setComplemento(request.complemento());
        factura.setCodigoCliente(request.codigoCliente());
        factura.setCodigoMetodoPago(request.codigoMetodoPago() != null ? request.codigoMetodoPago() : 1);
        factura.setCodigoMoneda(request.codigoMoneda() != null ? request.codigoMoneda() : 1);
        factura.setTipoCambio(request.tipoCambio() != null ? request.tipoCambio() : BigDecimal.ONE);
        factura.setCodigoActividad(config.getCodigoActividad());
        factura.setCodigoDocumentoSector(1);
        factura.setTipoEmision(tipoEmision);
        factura.setEstadoEmision(tipoEmision == 1 ? SiatEstadoEmision.PENDIENTE : SiatEstadoEmision.EN_PAQUETE);

        // Detalles
        List<SiatFacturaDetalle> detalles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int linea = 1;
        for (SiatEmisionDetalleRequest dr : request.detalles()) {
            SiatFacturaDetalle d = new SiatFacturaDetalle();
            d.setSiatFactura(factura);
            d.setNumeroLinea(linea++);
            d.setActividadEconomica(dr.actividadEconomica() != null ? dr.actividadEconomica() : config.getCodigoActividad());
            d.setCodigoProductoSin(dr.codigoProductoSin() != null ? dr.codigoProductoSin() : 84111);
            d.setCodigoProducto(dr.codigoProducto());
            d.setDescripcion(dr.descripcion());
            d.setCantidad(dr.cantidad());
            d.setUnidadMedida(dr.unidadMedida() != null ? dr.unidadMedida() : 58);
            d.setPrecioUnitario(dr.precioUnitario());
            d.setMontoDescuento(dr.montoDescuento() != null ? dr.montoDescuento() : BigDecimal.ZERO);
            BigDecimal subTotal = dr.cantidad().multiply(dr.precioUnitario()).subtract(d.getMontoDescuento());
            d.setSubTotal(subTotal);
            total = total.add(subTotal);
            detalles.add(d);
        }

        factura.setDetalles(detalles);
        factura.setImporteTotal(total);
        // En Bolivia el IVA está incluido en el precio; el importe sujeto a IVA = total
        factura.setImporteTotalSujetoIva(total);

        return factura;
    }

    /**
     * Construye un ZIP con los XMLs de las facturas y devuelve Base64.
     * En producción usar java.util.zip.ZipOutputStream.
     */
    private String buildZip(List<SiatFactura> facturas) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos);
            for (SiatFactura f : facturas) {
                String xml = f.getXmlContent() != null ? f.getXmlContent() : xmlBuilder.buildFacturaXml(f);
                java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(f.getCuf() + ".xml");
                zos.putNextEntry(entry);
                zos.write(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            zos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new BusinessException("Error generando ZIP: " + e.getMessage());
        }
    }

    private SiatPaqueteResponse toResponse(SiatPaquete p) {
        return new SiatPaqueteResponse(p.getId(), p.getCodigoSucursal(), p.getCodigoPuntoVenta(),
                p.getCantidadFacturas(), p.getTipoEmision(), p.getCodigoRecepcion(),
                p.getEstado(), p.getEstadoValidacion(), p.getMensajeSiat(),
                p.getFechaEmision(), p.getCreatedAt());
    }
}
