package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.inventario.api.ConsultaInventario;
import com.codefactory.supplychain.inventario.api.StockResumen;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarExistenciasPort;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarNodosPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * ACL hacia inventario.api: consulta existencias por nodo, resolviendo primero el nodo
 * de la tienda con {@link ConsultarNodosPort}.
 *
 * Módulo: reposicion.
 */
@Component
public class ConsultarExistenciasAdapter implements ConsultarExistenciasPort {

    private final ConsultaInventario consultaInventario;
    private final ConsultarNodosPort consultarNodos;

    public ConsultarExistenciasAdapter(ConsultaInventario consultaInventario,
                                       ConsultarNodosPort consultarNodos) {
        this.consultaInventario = consultaInventario;
        this.consultarNodos = consultarNodos;
    }

    @Override
    public Optional<Integer> consultarStockAlmacen(UUID tiendaId, UUID varianteId) {
        return consultarNodos.buscarNodoAlmacen(tiendaId)
                .flatMap(nodoId -> consultaInventario.consultarStock(nodoId, varianteId))
                .map(StockResumen::aLaMano);
    }

    @Override
    public Optional<Integer> consultarStockBodega(UUID tiendaId, UUID varianteId) {
        return consultarNodos.buscarNodoBodega(tiendaId)
                .flatMap(nodoId -> consultaInventario.consultarStock(nodoId, varianteId))
                .map(StockResumen::aLaMano);
    }

    @Override
    public List<UUID> variantesConInventario(UUID tiendaId) {
        return Stream.of(consultarNodos.buscarNodoAlmacen(tiendaId), consultarNodos.buscarNodoBodega(tiendaId))
                .flatMap(Optional::stream)
                .flatMap(nodoId -> consultaInventario.consultarExistenciasPorNodo(nodoId).stream())
                .map(StockResumen::varianteId)
                .distinct()
                .toList();
    }
}
