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
 *
 * <p>Pendiente: {@code ventaId} aún no se persiste como referencia_id del kardex porque
 * {@link InventarioUseCase#aplicarVenta} no lo recibe.
 */
@Component
@RequiredArgsConstructor
class InventarioMovimientosAdapter implements InventarioMovimientos {

    private static final String MOTIVO_ANULACION = "ANULACION_VENTA";

    private final InventarioUseCase inventarioUseCase;

    @Override
    public void descontarPorVenta(UUID nodoId, UUID varianteId, int cantidad, UUID ventaId, boolean forzado) {
        try {
            inventarioUseCase.aplicarVentaReferenciada(nodoId, varianteId, cantidad, forzado, ventaId);
        } catch (com.codefactory.supplychain.inventario.domain.exception.StockInsuficienteException e) {
            throw new StockInsuficienteException();
        }
    }

    @Override
    public void revertirPorAnulacion(UUID nodoId, UUID varianteId, int cantidad, UUID ventaId) {
        inventarioUseCase.revertirVenta(nodoId, varianteId, cantidad, MOTIVO_ANULACION, ventaId);
    }

    @Override
    public void trasladar(UUID nodoOrigenId, UUID nodoDestinoId, UUID varianteId, int cantidad, UUID traspasoId) {
        throw new UnsupportedOperationException("Pendiente de HU-40 (traspasos)");
    }
}
