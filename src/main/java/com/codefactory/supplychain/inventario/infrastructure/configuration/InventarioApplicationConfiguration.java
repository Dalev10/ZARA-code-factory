package com.codefactory.supplychain.inventario.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.BodegaTiendaRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.TiendaExistePort;
import com.codefactory.supplychain.inventario.application.service.bodegatienda.BodegaTiendaService;

/**
 * Configuración de beans de aplicación del módulo Inventario.
 */
@Configuration
public class InventarioApplicationConfiguration {

    @Bean
    public BodegaTiendaService bodegaTiendaService(
            BodegaTiendaRepositoryPort bodegaTiendaRepositoryPort,
            TiendaExistePort tiendaExistePort) {
        return new BodegaTiendaService(bodegaTiendaRepositoryPort, tiendaExistePort);
    }
}
