package com.codefactory.supplychain.ventas.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.ventas.application.port.out.DescontarStockPort;
import com.codefactory.supplychain.inventario.api.InventarioMovimientos;
import com.codefactory.supplychain.inventario.api.StockInsuficienteException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Anti-corruption layer hacia inventario: único archivo de ventas que importa inventario.api. */
@Component
@RequiredArgsConstructor
class DescontarStockAdapter implements DescontarStockPort {

    private final InventarioMovimientos inventarioMovimientos;

    @Override
    public boolean descontar(UUID almacenId, UUID varianteId, int cantidad, UUID ventaId, boolean forzado) {
        try {
            inventarioMovimientos.descontarPorVenta(almacenId, varianteId, cantidad, ventaId, forzado);
            return true;
        } catch (StockInsuficienteException e) {
            return false;
        }
    }
}
