package com.codefactory.supplychain.ventas.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.ConsultaInventario;
import com.codefactory.supplychain.inventario.api.StockResumen;
import com.codefactory.supplychain.ventas.application.port.out.StockAlmacenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Capa anticorrupción hacia {@code inventario.api.ConsultaInventario}. */
@Component
@RequiredArgsConstructor
class StockAlmacenAdapter implements StockAlmacenPort {

    private final ConsultaInventario consultaInventario;

    @Override
    public Optional<Integer> aLaMano(UUID almacenId, UUID varianteId) {
        return consultaInventario.consultarStock(almacenId, varianteId).map(StockResumen::aLaMano);
    }

    @Override
    public List<UUID> variantesConExistencias(UUID almacenId) {
        return consultaInventario.consultarExistenciasPorNodo(almacenId).stream().map(StockResumen::varianteId).toList();
    }
}
