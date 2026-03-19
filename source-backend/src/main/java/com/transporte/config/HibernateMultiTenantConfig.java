package com.transporte.config;

import com.transporte.security.multitenancy.SchemaMultiTenantConnectionProvider;
import com.transporte.security.multitenancy.TenantSchemaResolver;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class HibernateMultiTenantConfig {

    private final SchemaMultiTenantConnectionProvider multiTenantConnectionProvider;
    private final TenantSchemaResolver tenantSchemaResolver;

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return properties -> {
            properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
            properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantSchemaResolver);
        };
    }
}
