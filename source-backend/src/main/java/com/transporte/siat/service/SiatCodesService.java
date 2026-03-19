package com.transporte.siat.service;

import com.transporte.core.exception.BusinessException;
import com.transporte.siat.client.SiatCodesClient;
import com.transporte.siat.client.SiatSoapResponse;
import com.transporte.siat.dto.SiatCufdResponse;
import com.transporte.siat.dto.SiatCuisResponse;
import com.transporte.siat.entity.SiatConfig;
import com.transporte.siat.entity.SiatCufd;
import com.transporte.siat.entity.SiatCuis;
import com.transporte.siat.mapper.SiatCuisMapper;
import com.transporte.siat.mapper.SiatCufdMapper;
import com.transporte.siat.repository.SiatCufdRepository;
import com.transporte.siat.repository.SiatCuisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio para obtener CUIS y CUFD del SIN Bolivia.
 * Ambos códigos son prerrequisitos para cualquier emisión de facturas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiatCodesService {

    private final SiatCodesClient codesClient;
    private final SiatCuisRepository cuisRepository;
    private final SiatCufdRepository cufdRepository;
    private final SiatCuisMapper cuisMapper;
    private final SiatCufdMapper cufdMapper;

    // ==================== CUIS ====================

    /**
     * Obtiene un nuevo CUIS desde el SIN y lo persiste.
     * El CUIS se solicita una sola vez por sistema; tiene vigencia de 6 meses.
     */
    @Transactional
    public SiatCuisResponse obtenerCuis(SiatConfig config) {
        SiatSoapResponse response = codesClient.obtenerCuis(
                config.getNit(),
                config.getCodigoSistema(),
                config.getCodigoSucursal(),
                config.getCodigoPuntoVenta()
        );

        if (!response.isExitoso() || response.getDatos().get("cuis") == null) {
            throw new BusinessException("Error obteniendo CUIS del SIN: " + response.getMensaje());
        }

        String cuisCode = response.getDatos().get("cuis");
        String fechaVigenciaStr = response.getDatos().get("fechaVigencia");
        LocalDateTime fechaVigencia = parseFechaVigencia(fechaVigenciaStr);

        // Desactivar CUISes anteriores de esta sucursal/punto venta
        cuisRepository.findVigente(config.getId(), config.getCodigoSucursal(), config.getCodigoPuntoVenta())
                .ifPresent(old -> { old.setActivo(false); cuisRepository.save(old); });

        SiatCuis cuis = SiatCuis.builder()
                .siatConfig(config)
                .cuis(cuisCode)
                .fechaVigencia(fechaVigencia)
                .codigoSucursal(config.getCodigoSucursal())
                .codigoPuntoVenta(config.getCodigoPuntoVenta())
                .activo(true)
                .build();

        return cuisMapper.toResponse(cuisRepository.save(cuis));
    }

    /**
     * Devuelve el CUIS vigente o lanza excepción si no existe.
     * Llamar a obtenerCuis() primero si no existe.
     */
    public SiatCuis getCuisVigente(UUID configId, Integer sucursal, Integer puntoVenta) {
        return cuisRepository.findVigente(configId, sucursal, puntoVenta)
                .orElseThrow(() -> new BusinessException(
                        "No existe CUIS vigente. Solicite uno con POST /api/v1/siat/codigos/cuis"));
    }

    // ==================== CUFD ====================

    /**
     * Obtiene un nuevo CUFD desde el SIN y lo persiste.
     * El CUFD tiene vigencia diaria (24h) y se requiere para cada jornada de facturación.
     */
    @Transactional
    public SiatCufdResponse obtenerCufd(SiatConfig config) {
        SiatCuis cuisVigente = getCuisVigente(
                config.getId(), config.getCodigoSucursal(), config.getCodigoPuntoVenta());

        SiatSoapResponse response = codesClient.obtenerCufd(
                config.getNit(),
                config.getCodigoSistema(),
                cuisVigente.getCuis(),
                config.getCodigoSucursal(),
                config.getCodigoPuntoVenta()
        );

        if (!response.isExitoso() || response.getDatos().get("cufd") == null) {
            throw new BusinessException("Error obteniendo CUFD del SIN: " + response.getMensaje());
        }

        String cufdCode = response.getDatos().get("cufd");
        String codigoControl = response.getDatos().getOrDefault("codigoControl", "");
        String codigoParaQr = response.getDatos().get("codigoParaQr");
        String fechaVigenciaStr = response.getDatos().get("fechaVigencia");
        LocalDateTime fechaVigencia = parseFechaVigencia(fechaVigenciaStr);

        // Desactivar CUFDs anteriores
        cufdRepository.findVigente(config.getId(), config.getCodigoSucursal(), config.getCodigoPuntoVenta())
                .ifPresent(old -> { old.setActivo(false); cufdRepository.save(old); });

        SiatCufd cufd = SiatCufd.builder()
                .siatConfig(config)
                .cufd(cufdCode)
                .codigoControl(codigoControl)
                .codigoParaQr(codigoParaQr)
                .fechaVigencia(fechaVigencia)
                .codigoSucursal(config.getCodigoSucursal())
                .codigoPuntoVenta(config.getCodigoPuntoVenta())
                .activo(true)
                .build();

        return cufdMapper.toResponse(cufdRepository.save(cufd));
    }

    /**
     * Devuelve el CUFD vigente o lanza excepción si no existe o venció.
     */
    public SiatCufd getCufdVigente(UUID configId, Integer sucursal, Integer puntoVenta) {
        return cufdRepository.findVigente(configId, sucursal, puntoVenta)
                .orElseThrow(() -> new BusinessException(
                        "No existe CUFD vigente. Solicite uno con POST /api/v1/siat/codigos/cufd"));
    }

    // ==================== UTIL ====================

    private LocalDateTime parseFechaVigencia(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) {
            // Fallback: 24 horas para CUFD, 6 meses para CUIS
            return LocalDateTime.now().plusHours(24);
        }
        try {
            // El SIN devuelve fechas en formato ISO o "yyyy-MM-dd HH:mm:ss"
            String cleaned = fechaStr.trim().replace(" ", "T");
            if (cleaned.contains("T")) {
                return LocalDateTime.parse(cleaned.substring(0, 19),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            }
            return LocalDateTime.parse(cleaned.substring(0, 10),
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    .plusHours(23).plusMinutes(59);
        } catch (Exception e) {
            log.warn("No se pudo parsear fecha de vigencia '{}', usando +24h", fechaStr);
            return LocalDateTime.now().plusHours(24);
        }
    }
}
