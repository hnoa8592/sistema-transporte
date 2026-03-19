package com.transporte.encomiendas;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal Spring Boot application context for integration tests in the encomiendas module.
 *
 * This class is intentionally placed in the test source set so it is NOT bundled
 * in the production artifact.  It provides just enough configuration to boot a
 * Spring context with H2 and execute JPA-backed service tests.
 */
@SpringBootApplication
@EnableJpaAuditing
@EntityScan(basePackages = {"com.transporte.encomiendas", "com.transporte.core"})
@EnableJpaRepositories(basePackages = "com.transporte.encomiendas")
@ComponentScan(basePackages = {"com.transporte.encomiendas", "com.transporte.core"})
public class EncomiendasTestConfig {}
