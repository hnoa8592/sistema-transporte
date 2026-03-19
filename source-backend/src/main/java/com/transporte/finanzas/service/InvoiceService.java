package com.transporte.finanzas.service;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditAction;
import com.transporte.core.exception.BusinessException;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.core.response.PageResponse;
import com.transporte.core.util.CodeGenerator;
import com.transporte.finanzas.dto.*;
import com.transporte.finanzas.entity.Invoice;
import com.transporte.finanzas.entity.InvoiceItem;
import com.transporte.finanzas.enums.InvoiceStatus;
import com.transporte.finanzas.mapper.InvoiceMapper;
import com.transporte.finanzas.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final SystemParameterService systemParameterService;

    public PageResponse<InvoiceResponse> findAll(Pageable pageable) {
        return PageResponse.of(invoiceRepository.findAll(pageable).map(invoiceMapper::toResponse));
    }

    public InvoiceResponse findById(UUID id) {
        return invoiceMapper.toResponse(invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id)));
    }

    @Auditable(action = AuditAction.CREATE, entityType = "Invoice", description = "Emisión de factura")
    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        String prefix = systemParameterService.getValue("INVOICE_PREFIX", "INV");
        BigDecimal taxPercent = request.taxPercent() != null ? request.taxPercent() :
                new BigDecimal(systemParameterService.getValue("TAX_PERCENT", "0"));

        Long nextSeq = (invoiceRepository.findMaxSequence() == null ? 0L : invoiceRepository.findMaxSequence()) + 1;
        String invoiceNumber = CodeGenerator.generateInvoiceNumber(prefix, nextSeq);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCustomerId(request.customerId());
        invoice.setCustomerName(request.customerName());
        invoice.setCustomerDocument(request.customerDocument());
        invoice.setTaxPercent(taxPercent);
        invoice.setStatus(InvoiceStatus.EMITIDA);

        List<InvoiceItem> items = request.items().stream().map(itemReq -> {
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setDescription(itemReq.description());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(itemReq.unitPrice());
            item.setSubtotal(itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity())));
            return item;
        }).toList();

        BigDecimal subtotal = items.stream().map(InvoiceItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = subtotal.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxAmount);

        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotal(total);
        invoice.setItems(items);

        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Auditable(action = AuditAction.CANCEL, entityType = "Invoice", description = "Anulación de factura")
    @Transactional
    public InvoiceResponse cancel(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        if (invoice.getStatus() == InvoiceStatus.ANULADA) {
            throw new BusinessException("La factura ya está anulada");
        }
        invoice.setStatus(InvoiceStatus.ANULADA);
        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }
}
