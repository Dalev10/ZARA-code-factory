package com.codefactory.supplychain.inventario.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.api.ConsultaInventario;
import com.codefactory.supplychain.inventario.api.StockResumen;
import com.codefactory.supplychain.inventario.application.port.out.ExistenciasRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementa {@link ConsultaInventario} (API pública de solo lectura) sobre
 * {@link ExistenciasRepositoryPort}. Consumidores: reposicion (stock de Almacén y
 * Bodega) y ventas.
 * <p>
 * Sin fila de inventario para (nodo, variante) devuelve {@code Optional.empty()}:
 * la variante no tiene carga inicial en el nodo, no se interpreta como stock cero.
 * {@link #consultarExistenciasPorNodo} incluye las variantes con saldo cero y las
 * ordena por varianteId para que el resultado sea determinista.
 *
 * Módulo: inventario.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ConsultaInventarioAdapter implements ConsultaInventario {

    private final ExistenciasRepositoryPort existencias;

    @Override
    public Optional<StockResumen> consultarStock(UUID nodoId, UUID varianteId) {
        Objects.requireNonNull(nodoId, "nodoId es obligatorio");
        Objects.requireNonNull(varianteId, "varianteId es obligatorio");
        return existencias.buscarPorNodoYVariante(nodoId, varianteId);
    }

    @Override
    public List<StockResumen> consultarExistenciasPorNodo(UUID nodoId) {
        Objects.requireNonNull(nodoId, "nodoId es obligatorio");
        return existencias.listarPorNodo(nodoId).stream()
                .sorted(Comparator.comparing(StockResumen::varianteId))
                .toList();
    }
}
