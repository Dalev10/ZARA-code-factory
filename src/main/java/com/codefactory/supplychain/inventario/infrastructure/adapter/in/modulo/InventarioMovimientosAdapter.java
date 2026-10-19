package com.codefactory.supplychain.inventario.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.api.InventarioMovimientos;
import com.codefactory.supplychain.inventario.api.StockInsuficienteException;
import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementa {@link InventarioMovimientos} (API pública) delegando en {@link InventarioUseCase}.
 * Traduce los errores de dominio a los de la frontera. Sin reglas de negocio.
 */
@Component
@RequiredArgsConstructor
class InventarioMovimientosAdapter implements InventarioMovimientos {

    private final InventarioUseCase inventarioUseCase;

    @Override
    public void descontarPorVenta(UUID nodoId, UUID varianteId, int cantidad, UUID ventaId, boolean forzado) {
        try {
            inventarioUseCase.aplicarVenta(nodoId, varianteId, cantidad, forzado, ventaId);
        } catch (com.codefactory.supplychain.inventario.domain.exception.StockInsuficienteException e) {
            throw new StockInsuficienteException();
        }
    }

    @Override
    public void revertirPorAnulacion(UUID nodoId, UUID varianteId, int cantidad, UUID ventaId) {
        inventarioUseCase.revertir(nodoId, varianteId, cantidad, ventaId);
    }

    @Override
    public void trasladar(UUID nodoOrigenId, UUID nodoDestinoId, UUID varianteId, int cantidad, UUID traspasoId) {
        try {
            inventarioUseCase.trasladar(nodoOrigenId, nodoDestinoId, varianteId, cantidad, traspasoId);
        } catch (com.codefactory.supplychain.inventario.domain.exception.StockInsuficienteException e) {
            throw new StockInsuficienteException();
        }
    }
}
