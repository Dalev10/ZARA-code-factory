package com.codefactory.supplychain.logistica.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.ConsultaInventario;
import com.codefactory.supplychain.inventario.api.ConsultaNodos;
import com.codefactory.supplychain.inventario.api.MovimientosAbastecimiento;
import com.codefactory.supplychain.inventario.api.StockResumen;
import com.codefactory.supplychain.logistica.application.port.out.InventarioDespachoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ACL hacia {@code inventario.api} para el despacho (HU-46): lee el {@code a_la_mano} del nodo del
 * CD y registra la salida.
 *
 * Módulo: logistica.
 */
@Component
@RequiredArgsConstructor
class InventarioDespachoAdapter implements InventarioDespachoPort {

    private final ConsultaNodos consultaNodos;
    private final ConsultaInventario consultaInventario;
    private final MovimientosAbastecimiento movimientos;

    @Override
    public int aLaManoEnCd(UUID cdId, UUID varianteId) {
        return consultaNodos.buscarNodoCd(cdId)
                .flatMap(nodo -> consultaInventario.consultarStock(nodo, varianteId))
                .map(StockResumen::aLaMano)
                .orElse(0);
    }

    @Override
    public void registrarDespacho(UUID cdId, UUID varianteId, int cantidadQueSale, int reservaALiberar,
                                  UUID cicloId) {
        movimientos.registrarDespacho(cdId, varianteId, cantidadQueSale, reservaALiberar, cicloId);
    }
}
