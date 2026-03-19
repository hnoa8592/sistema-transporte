package com.transporte.siat.service;

import com.transporte.core.exception.BusinessException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Servicio para registro de Eventos Significativos SIAT Bolivia.
 * Los eventos se usan para justificar emisión offline (fuera de línea).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiatEventoService {

    private final SiatCodesClient codesClient;
    private final SiatCodesService codesService;
    private final SiatEventoRepository eventoRepository;
    private final SiatConfigRepository configRepository;
    private final SiatEventoMapper eventoMapper;

    private static final DateTimeFormatter SIAT_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public PageResponse<SiatEventoResponse> findByConfig(UUID configId, Pageable pageable) {
        return PageResponse.of(eventoRepository.findBySiatConfigId(configId, pageable)
                .map(eventoMapper::toResponse));
    }

    /**
     * Registra un evento significativo en el SIN.
     * Requerido antes de emitir facturas en modo offline.
     */
    @Transactional
    public SiatEventoResponse registrar(SiatEventoRequest request) {
        SiatConfig config = configRepository.findById(request.siatConfigId())
                .orElseThrow(() -> new ResourceNotFoundException("SiatConfig", request.siatConfigId()));

        int sucursal = request.codigoSucursal() != null ? request.codigoSucursal() : config.getCodigoSucursal();
        Integer pv = request.codigoPuntoVenta() != null ? request.codigoPuntoVenta() : config.getCodigoPuntoVenta();

        SiatCuis cuis = codesService.getCuisVigente(config.getId(), sucursal, pv);
        SiatCufd cufd = codesService.getCufdVigente(config.getId(), sucursal, pv);

        String fechaInicio = request.fechaInicio().format(SIAT_DT);
        String fechaFin = request.fechaFin() != null ? request.fechaFin().format(SIAT_DT) : null;

        SiatSoapResponse siatResp = codesClient.registrarEvento(
                config.getNit(), config.getCodigoSistema(),
                cuis.getCuis(), sucursal, pv,
                request.codigoEvento(),
                request.descripcion() != null ? request.descripcion() : "",
                fechaInicio, fechaFin);

        SiatEvento evento = SiatEvento.builder()
                .siatConfig(config)
                .siatCufd(cufd)
                .codigoEvento(request.codigoEvento())
                .descripcion(request.descripcion())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .codigoSucursal(sucursal)
                .codigoPuntoVenta(pv)
                .codigoRecepcion(siatResp.getCodigoRecepcion())
                .estado(siatResp.isExitoso() ? "REGISTRADO" : "ERROR")
                .mensajeSiat(siatResp.getMensaje())
                .build();

        if (!siatResp.isExitoso()) {
            log.warn("Evento no registrado en el SIN: {}", siatResp.getMensaje());
        }

        return eventoMapper.toResponse(eventoRepository.save(evento));
    }
}
