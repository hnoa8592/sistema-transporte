package com.transporte;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.transporte")
@EnableConfigurationProperties
@EnableCaching
@EnableScheduling
public class TransporteApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransporteApplication.class, args);
    }
}
