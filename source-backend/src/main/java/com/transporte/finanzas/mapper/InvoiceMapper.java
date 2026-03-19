package com.transporte.finanzas.mapper;

import com.transporte.finanzas.dto.InvoiceItemResponse;
import com.transporte.finanzas.dto.InvoiceResponse;
import com.transporte.finanzas.entity.Invoice;
import com.transporte.finanzas.entity.InvoiceItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {
    InvoiceResponse toResponse(Invoice invoice);
    InvoiceItemResponse toItemResponse(InvoiceItem item);
}
