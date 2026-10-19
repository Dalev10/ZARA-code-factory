package com.codefactory.supplychain.reposicion.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Ejecutor propio para los listeners de reposicion que reaccionan a eventos de
 * inventario.
 * <p>
 * El listener corre AFTER_COMMIT con una transacción REQUIRES_NEW. Si corriera en el
 * mismo hilo de la venta, ese hilo retendría su conexión mientras pide otra; con
 * varias ventas simultáneas y un pool pequeño (5 en producción) los hilos se
 * bloquean entre sí hasta agotar el timeout de Hikari. Al pasarlo a este ejecutor,
 * la venta libera su conexión antes de que el listener abra la suya.
 * <p>
 * Un solo hilo: los traspasos automáticos se evalúan en serie, sin competir entre
 * ellos por el mismo stock de Bodega.
 *
 * Módulo: reposicion.
 */
@Configuration
@EnableAsync
public class ConfiguracionEventosReposicion {

    public static final String EJECUTOR = "eventosReposicionExecutor";

    @Bean(name = EJECUTOR)
    ThreadPoolTaskExecutor eventosReposicionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("eventos-reposicion-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
