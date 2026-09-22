package com.codefactory.supplychain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del monolito modular.
 *
 * Los módulos de dominio (identity, catalogo, inventario, ventas,
 * reposicion, logistica) se auto-detectan por estar bajo el mismo
 * paquete raíz "com.codefactory.supplychain".
 */
@SpringBootApplication
public class SupplyChainMvpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupplyChainMvpApplication.class, args);
    }
}
