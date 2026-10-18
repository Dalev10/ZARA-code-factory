package com.codefactory.supplychain.ventas.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.ventas.application.port.out.StockAlmacenPort;
import com.codefactory.supplychain.ventas.domain.exception.IntegracionInventarioPendienteException;
import com.codefactory.supplychain.inventario.api.ConsultaInventario;
import com.codefactory.supplychain.inventario.api.StockResumen;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Capa anticorrupción hacia {@code inventario.api.ConsultaInventario}. Hoy la interfaz existe pero
 * Inventario no registra una implementación: mientras no haya bean, falla con
 * {@link IntegracionInventarioPendienteException} (HTTP 503) en vez de devolver stock ficticio.
 * Cuando Inventario la implemente, este adaptador funciona sin cambios.
 */
@Component
public class StockAlmacenAdapter implements StockAlmacenPort {

    private final ObjectProvider<ConsultaInventario> consultaInventario;

    public StockAlmacenAdapter(ObjectProvider<ConsultaInventario> consultaInventario) {
        this.consultaInventario = consultaInventario;
    }

    @Override
    public Optional<Integer> aLaMano(UUID almacenId, UUID varianteId) {
        return consulta().consultarStock(almacenId, varianteId).map(StockResumen::aLaMano);
    }

    @Override
    public List<UUID> variantesConExistencias(UUID almacenId) {
        return consulta().consultarExistenciasPorNodo(almacenId).stream().map(StockResumen::varianteId).toList();
    }

    private ConsultaInventario consulta() {
        ConsultaInventario consulta = consultaInventario.getIfAvailable();
        if (consulta == null) {
            throw new IntegracionInventarioPendienteException("inventario.api.ConsultaInventario");
        }
        return consulta;
    }
}
