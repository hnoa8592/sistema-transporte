package com.transporte.tenants.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final DataSource dataSource;

    public void provisionTenant(String schemaName) {
        createSchema(schemaName);
        runMigrations(schemaName);
        log.info("Esquema de la empresa '{}' aprovisionado exitosamente", schemaName);
    }

    private void createSchema(String schemaName) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
            log.info("Esquema '{}' creado", schemaName);
        } catch (Exception e) {
            throw new RuntimeException("Error al crear el esquema: " + schemaName, e);
        }
    }

    private void runMigrations(String schemaName) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration/tenant")
                .table("flyway_schema_history")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Migraciones aplicadas al esquema '{}'", schemaName);
    }
}
