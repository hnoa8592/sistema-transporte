package com.transporte.encomiendas.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.core.util.CodeGenerator;
import com.transporte.encomiendas.dto.*;
import com.transporte.encomiendas.entity.Parcel;
import com.transporte.encomiendas.entity.ParcelTracking;
import com.transporte.encomiendas.enums.ParcelStatus;
import com.transporte.encomiendas.mapper.ParcelMapper;
import com.transporte.encomiendas.mapper.ParcelTrackingMapper;
import com.transporte.encomiendas.repository.ParcelRepository;
import com.transporte.encomiendas.repository.ParcelTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ParcelService {

    private final ParcelRepository parcelRepository;
    private final ParcelTrackingRepository trackingRepository;
    private final ParcelMapper parcelMapper;
    private final ParcelTrackingMapper trackingMapper;
    private final ApplicationEventPublisher eventPublisher;

    public PageResponse<ParcelResponse> findAll(Pageable pageable) {
        return PageResponse.of(parcelRepository.findAll(pageable).map(parcelMapper::toResponse));
    }

    public PageResponse<ParcelResponse> findByStatus(ParcelStatus status, Pageable pageable) {
        return PageResponse.of(parcelRepository.findAllByStatus(status, pageable).map(parcelMapper::toResponse));
    }

    public ParcelResponse findById(UUID id) {
        return parcelMapper.toResponse(findParcelById(id));
    }

    public ParcelResponse findByTrackingCode(String trackingCode) {
        return parcelMapper.toResponse(
                parcelRepository.findByTrackingCode(trackingCode)
                        .orElseThrow(() -> new ResourceNotFoundException("Encomienda con código de seguimiento " + trackingCode + " no encontrada"))
        );
    }

    public List<ParcelTrackingResponse> getTracking(UUID parcelId) {
        if (!parcelRepository.existsById(parcelId)) {
            throw new ResourceNotFoundException("Parcel", parcelId);
        }
        return trackingRepository.findByParcelIdOrderByTimestampDesc(parcelId)
                .stream().map(trackingMapper::toResponse).toList();
    }

    public List<ParcelTrackingResponse> getTrackingByCode(String trackingCode) {
        Parcel parcel = parcelRepository.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel with tracking code " + trackingCode + " not found"));
        return trackingRepository.findByParcelIdOrderByTimestampDesc(parcel.getId())
                .stream().map(trackingMapper::toResponse).toList();
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Parcel", description = "Recepción de nueva encomienda")
    @Transactional
    public ParcelResponse create(ParcelRequest request) {
        Parcel parcel = parcelMapper.toEntity(request);
        parcel.setTrackingCode(CodeGenerator.generateTrackingCode("PCL"));
        parcel.setStatus(ParcelStatus.RECIBIDO);
        parcel = parcelRepository.save(parcel);

        // Register initial tracking
        addTracking(parcel.getId(), ParcelStatus.RECIBIDO, "Origen", "Encomienda recibida en origen");

        log.info("Encomienda {} creada con código de seguimiento {}", parcel.getId(), parcel.getTrackingCode());
        return parcelMapper.toResponse(parcel);
    }

    @Auditable(action = AuditAction.STATUS_CHANGE, entityType = "Parcel", description = "Actualización de estado de encomienda")
    @Transactional
    public ParcelResponse updateStatus(UUID id, UpdateParcelStatusRequest request) {
        Parcel parcel = findParcelById(id);
        validateStatusTransition(parcel.getStatus(), request.status());

        parcel.setStatus(request.status());
        parcel = parcelRepository.save(parcel);

        addTracking(parcel.getId(), request.status(), request.location(), request.notes());

        log.info("Encomienda {} actualizada al estado {}", parcel.getTrackingCode(), request.status());
        return parcelMapper.toResponse(parcel);
    }

    private void addTracking(UUID parcelId, ParcelStatus status, String location, String notes) {
        ParcelTracking tracking = ParcelTracking.builder()
                .parcelId(parcelId)
                .status(status)
                .location(location)
                .notes(notes)
                .timestamp(LocalDateTime.now())
                .build();
        trackingRepository.save(tracking);
    }

    private void validateStatusTransition(ParcelStatus current, ParcelStatus next) {
        boolean valid = switch (current) {
            case RECIBIDO -> next == ParcelStatus.EN_TRANSITO;
            case EN_TRANSITO -> next == ParcelStatus.EN_DESTINO || next == ParcelStatus.DEVUELTO;
            case EN_DESTINO -> next == ParcelStatus.ENTREGADO || next == ParcelStatus.DEVUELTO;
            case ENTREGADO, DEVUELTO -> false;
        };
        if (!valid) {
            throw new BusinessException("Transición de estado inválida de " + current + " a " + next);
        }
    }

    private Parcel findParcelById(UUID id) {
        return parcelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parcel", id));
    }
}
