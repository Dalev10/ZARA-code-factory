package com.codefactory.supplychain.ventas.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Reloj inyectable para que los servicios sean deterministas en pruebas. */
@Configuration
public class VentasConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock relojDelSistema() {
        return Clock.systemUTC();
    }
}
