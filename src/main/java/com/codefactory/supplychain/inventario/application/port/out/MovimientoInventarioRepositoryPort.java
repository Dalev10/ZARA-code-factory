package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.api.MovimientoKardex;
import com.codefactory.supplychain.inventario.application.dto.MovimientoKardexConsulta;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import com.codefactory.supplychain.inventario.domain.model.TipoMovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de persistencia para los movimientos del kardex.
 */
public interface MovimientoInventarioRepositoryPort {

    MovimientoInventario guardar(
            UUID nodoId,
            UUID varianteId,
            MovimientoInventario movimiento);

    /**
     * Movimientos de un nodo entre {@code desde} y {@code hasta} (inclusive),
     * ordenados por fecha. {@code tipo} es opcional: {@code null} trae todos.
     */
    Page<MovimientoKardexConsulta> buscarKardex(
            UUID nodoId,
            TipoMovimientoInventario tipo,
            Instant desde,
            Instant hasta,
            Pageable pageable);

    /**
     * Movimientos de una variante en un nodo entre {@code desde} y {@code hasta}
     * (inclusive), del más antiguo al más reciente.
     */
    List<MovimientoKardex> buscarPorNodoYVariante(UUID nodoId, UUID varianteId, Instant desde, Instant hasta);
}
