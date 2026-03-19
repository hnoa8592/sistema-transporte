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
import com.transporte.siat.entity.SiatFactura;
import com.transporte.siat.enums.SiatEstadoEmision;
import com.transporte.siat.mapper.SiatEmisionMapper;
import com.transporte.siat.repository.SiatFacturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de anulación y reversión de facturas SIAT Bolivia.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiatAnulacionService {

    private final SiatEmisionClient emisionClient;
    private final SiatCodesService codesService;
    private final SiatFacturaRepository facturaRepository;
    private final SiatEmisionMapper emisionMapper;

    /**
     * Anula una factura emitida en el SIN.
     * Solo se pueden anular facturas con estado VALIDO.
     */
    @Transactional
    public SiatEmisionResponse anular(SiatAnulacionRequest request) {
        SiatFactura factura = facturaRepository.findById(request.siatFacturaId())
                .orElseThrow(() -> new ResourceNotFoundException("SiatFactura", request.siatFacturaId()));

        if (factura.getEstadoEmision() == SiatEstadoEmision.ANULADO) {
            throw new BusinessException("La factura ya está anulada");
        }
        if (factura.getCuf() == null) {
            throw new BusinessException("La factura no tiene CUF asignado, no puede anularse en el SIN");
        }

        SiatConfig config = factura.getSiatConfig();
        SiatCufd cufd = factura.getSiatCufd();

        SiatSoapResponse siatResp = emisionClient.anulacionFactura(
                config.getNit(), config.getCodigoSistema(),
                codesService.getCuisVigente(config.getId(),
                        config.getCodigoSucursal(), config.getCodigoPuntoVenta()).getCuis(),
                cufd.getCufd(),
                config.getCodigoSucursal(), config.getCodigoPuntoVenta(),
                factura.getCuf(), request.codigoMotivo());

        factura.setMensajeSiat(siatResp.getMensaje());
        if (siatResp.isExitoso()) {
            factura.setEstadoEmision(SiatEstadoEmision.ANULADO);
            log.info("Factura {} anulada exitosamente en el SIN", factura.getCuf());
        } else {
            log.warn("Anulación rechazada por el SIN: {}", siatResp.getMensaje());
            throw new BusinessException("El SIN rechazó la anulación: " + siatResp.getMensaje());
        }

        return emisionMapper.toResponse(facturaRepository.save(factura));
    }

    /**
     * Revierte la anulación de una factura.
     * Solo se pueden revertir facturas con estado ANULADO.
     */
    @Transactional
    public SiatEmisionResponse revertir(SiatReversionRequest request) {
        SiatFactura factura = facturaRepository.findById(request.siatFacturaId())
                .orElseThrow(() -> new ResourceNotFoundException("SiatFactura", request.siatFacturaId()));

        if (factura.getEstadoEmision() != SiatEstadoEmision.ANULADO) {
            throw new BusinessException("Solo se pueden revertir facturas con estado ANULADO");
        }

        SiatConfig config = factura.getSiatConfig();
        SiatCufd cufd = factura.getSiatCufd();

        SiatSoapResponse siatResp = emisionClient.reversionAnulacion(
                config.getNit(), config.getCodigoSistema(),
                codesService.getCuisVigente(config.getId(),
                        config.getCodigoSucursal(), config.getCodigoPuntoVenta()).getCuis(),
                cufd.getCufd(),
                config.getCodigoSucursal(), config.getCodigoPuntoVenta(),
                factura.getCuf());

        factura.setMensajeSiat(siatResp.getMensaje());
        if (siatResp.isExitoso()) {
            factura.setEstadoEmision(SiatEstadoEmision.REVERTIDO);
            log.info("Reversión de factura {} exitosa en el SIN", factura.getCuf());
        } else {
            throw new BusinessException("El SIN rechazó la reversión: " + siatResp.getMensaje());
        }

        return emisionMapper.toResponse(facturaRepository.save(factura));
    }
}
