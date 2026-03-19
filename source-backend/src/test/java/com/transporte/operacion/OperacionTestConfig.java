package com.transporte.operacion;

import com.transporte.config.CacheConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal Spring Boot application context for integration tests in the operacion module.
 *
 * Scans only the operacion and core packages to keep the context lightweight.
 * @EnableJpaAuditing is provided by AuditConfig (in core.audit), already picked
 * up by @ComponentScan — no need to declare it here (avoids duplicate bean error).
 * Imports CacheConfig explicitly to support @Cacheable annotations in ScheduleService.
 */
@SpringBootApplication
@EnableCaching
@Import(CacheConfig.class)
@EntityScan(basePackages = {"com.transporte.operacion", "com.transporte.core"})
@EnableJpaRepositories(basePackages = "com.transporte.operacion")
@ComponentScan(basePackages = {"com.transporte.operacion", "com.transporte.core"})
public class OperacionTestConfig {}
