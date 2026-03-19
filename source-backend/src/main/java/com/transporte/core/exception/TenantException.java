package com.transporte.core.exception;

import org.springframework.http.HttpStatus;

public class TenantException extends BusinessException {
    public TenantException(String message) {
        super(message, "TENANT_ERROR", HttpStatus.FORBIDDEN);
    }
}
