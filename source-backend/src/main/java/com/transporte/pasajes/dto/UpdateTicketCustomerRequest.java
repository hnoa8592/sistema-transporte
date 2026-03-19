package com.transporte.pasajes.dto;

import java.util.UUID;

public record UpdateTicketCustomerRequest(
        UUID customerId,
        String passengerName,
        String passengerDocument
) {}
