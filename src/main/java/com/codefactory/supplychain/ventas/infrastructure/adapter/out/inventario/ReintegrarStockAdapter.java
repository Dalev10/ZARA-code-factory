package com.codefactory.supplychain.ventas.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.InventarioMovimientos;
import com.codefactory.supplychain.ventas.application.port.out.ReintegrarStockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Anti-corruption layer hacia el contrato de movimientos de Inventario. */
@Component
@RequiredArgsConstructor
class ReintegrarStockAdapter implements ReintegrarStockPort {

    private final InventarioMovimientos inventarioMovimientos;

    @Override
    public void reintegrar(UUID almacenId, UUID varianteId, int cantidad, UUID ventaId) {
        inventarioMovimientos.revertirPorAnulacion(almacenId, varianteId, cantidad, ventaId);
    }
}
