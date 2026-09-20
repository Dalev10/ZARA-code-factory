package com.codefactory.supplychain.inventario.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.BodegaTiendaRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.InventarioPorNodoPort;
import com.codefactory.supplychain.inventario.application.port.out.bodegatienda.TiendaConsultaPort;
import com.codefactory.supplychain.inventario.application.service.bodegatienda.BodegaTiendaService;

/**
 * Configuración de beans de aplicación del módulo Inventario.
 */
@Configuration
public class InventarioApplicationConfiguration {

    @Bean
    public BodegaTiendaService bodegaTiendaService(
            BodegaTiendaRepositoryPort bodegaTiendaRepositoryPort,
            TiendaConsultaPort tiendaConsultaPort,
            InventarioPorNodoPort inventarioPorNodoPort) {
        return new BodegaTiendaService(
                bodegaTiendaRepositoryPort, tiendaConsultaPort, inventarioPorNodoPort);
    }
}
