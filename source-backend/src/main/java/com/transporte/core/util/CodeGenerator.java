package com.transporte.core.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class CodeGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private CodeGenerator() {}

    public static String generateTrackingCode(String prefix) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return prefix + "-" + timestamp + "-" + random;
    }

    public static String generateTicketCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static String generateInvoiceNumber(String prefix, long sequence) {
        return prefix + "-" + String.format("%08d", sequence);
    }
}
