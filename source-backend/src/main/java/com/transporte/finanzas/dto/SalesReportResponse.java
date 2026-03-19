package com.transporte.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesReportResponse(
        LocalDate date,
        long totalTickets,
        BigDecimal totalRevenue,
        long totalParcels,
        BigDecimal totalParcelRevenue
) {}
