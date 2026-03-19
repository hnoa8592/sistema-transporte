package com.transporte.auditoria.aspect;

import com.transporte.auditoria.annotation.Auditable;
import com.transporte.auditoria.enums.AuditStatus;
import com.transporte.auditoria.service.AuditService;
import com.transporte.security.multitenancy.TenantContext;
import com.transporte.security.multitenancy.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return pjp.proceed();
        }

        String userId = UserContext.getCurrentUserId();
        String username = extractUsername();
        String httpMethod = null;
        String endpoint = null;
        String ipAddress = null;

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            httpMethod = attrs.getRequest().getMethod();
            endpoint = attrs.getRequest().getRequestURI();
            ipAddress = getClientIp(attrs);
        }

        String description = auditable.description().isBlank() ? null : auditable.description();

        try {
            Object result = pjp.proceed();
            String entityId = extractEntityId(result, pjp.getArgs());
            auditService.log(tenantId, userId, username,
                    auditable.action(), auditable.entityType(), entityId,
                    httpMethod, endpoint, ipAddress,
                    AuditStatus.SUCCESS, null, description);
            return result;
        } catch (Throwable ex) {
            String entityId = extractEntityIdFromArgs(pjp.getArgs());
            auditService.log(tenantId, userId, username,
                    auditable.action(), auditable.entityType(), entityId,
                    httpMethod, endpoint, ipAddress,
                    AuditStatus.FAILURE, ex.getMessage(), description);
            throw ex;
        }
    }

    private String extractUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return null;
    }

    private String extractEntityId(Object result, Object[] args) {
        if (result != null) {
            try {
                Object idValue = result.getClass().getMethod("id").invoke(result);
                if (idValue != null) {
                    return idValue.toString();
                }
            } catch (Exception ignored) {
                // result does not have id() method
            }
        }
        return extractEntityIdFromArgs(args);
    }

    private String extractEntityIdFromArgs(Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof UUID) {
                    return arg.toString();
                }
            }
        }
        return null;
    }

    private String getClientIp(ServletRequestAttributes attrs) {
        String ip = attrs.getRequest().getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = attrs.getRequest().getRemoteAddr();
        }
        return ip;
    }
}
