package com.transporte.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Configuración de caché en memoria con Caffeine.
 *
 * TTLs por módulo según frecuencia de cambio:
 *   - resources, fleets, siat-catalogos : 60 min  (muy estables)
 *   - routes                             : 30 min  (estables)
 *   - buses, drivers, profiles,
 *     siat-config                        : 15 min  (cambio moderado)
 *   - schedules, parameters              : 10 min  (pueden cambiar operativamente)
 *   - users                              : 5  min  (datos sensibles, rotación frecuente)
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                // ── Operación ──────────────────────────────────────────────
                build("routes",          200,  30, TimeUnit.MINUTES),
                build("fleets",          100,  60, TimeUnit.MINUTES),
                build("buses",           500,  15, TimeUnit.MINUTES),
                build("drivers",         500,  15, TimeUnit.MINUTES),
                build("schedules",       500,  10, TimeUnit.MINUTES),
                // ── Usuarios / Seguridad ────────────────────────────────────
                build("users",          1000,   5, TimeUnit.MINUTES),
                build("profiles",        100,  15, TimeUnit.MINUTES),
                build("resources",       500,  60, TimeUnit.MINUTES),
                // ── Finanzas ───────────────────────────────────────────────
                build("parameters",      200,  10, TimeUnit.MINUTES),
                // ── SIAT ───────────────────────────────────────────────────
                build("siat-config",      50,  15, TimeUnit.MINUTES),
                build("siat-catalogos", 1000,  60, TimeUnit.MINUTES)
        ));
        return manager;
    }

    private CaffeineCache build(String name, int maxSize, long duration, TimeUnit unit) {
        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterWrite(duration, unit)
                        .recordStats()
                        .build());
    }
}
