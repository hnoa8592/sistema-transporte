package com.transporte.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FlywayConfig {

    private final DataSource dataSource;

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Migrando esquema público...");
            flyway.migrate();
        };
    }

    /**
     * Al arrancar la aplicación, aplica las migraciones pendientes a todos los
     * schemas de tenants registrados en public.tenants.
     * Esto garantiza que nuevas migraciones (V7, V8, etc.) se propaguen a tenants
     * existentes sin necesidad de re-provisionarlos manualmente.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void migrateAllTenantSchemas() {
        List<String> schemas = loadTenantSchemas();
        if (schemas.isEmpty()) {
            log.info("No se encontraron esquemas de empresas para migrar.");
            return;
        }
        log.info("Aplicando migraciones pendientes a {} esquema(s): {}", schemas.size(), schemas);
        for (String schema : schemas) {
            try {
                migrateTenantSchema(schema);
            } catch (Exception e) {
                log.error("Error al migrar el esquema de la empresa '{}': {}", schema, e.getMessage(), e);
            }
        }
    }

    public void migrateTenantSchema(String schemaName) {
        log.info("Migrando esquema para la empresa: {}", schemaName);
        Flyway tenantFlyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .table("flyway_schema_history")
                .baselineOnMigrate(true)
                .load();
        tenantFlyway.migrate();
    }

    private List<String> loadTenantSchemas() {
        List<String> schemas = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT schema_name FROM public.tenants WHERE active = true")) {
            while (rs.next()) {
                schemas.add(rs.getString("schema_name"));
            }
        } catch (Exception e) {
            log.warn("No se pudieron cargar los esquemas de empresas para migración: {}", e.getMessage());
        }
        return schemas;
    }
}
