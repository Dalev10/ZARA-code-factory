package com.codefactory.supplychain.inventario.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.api.ConsultaKardex;
import com.codefactory.supplychain.inventario.api.MovimientoKardex;
import com.codefactory.supplychain.inventario.application.port.out.MovimientoInventarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementa {@link ConsultaKardex} (API pública de solo lectura) sobre el kardex.
 * Consumidor: ventas, para reconciliar ventanas de quiebre (D-49).
 *
 * Módulo: inventario.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ConsultaKardexAdapter implements ConsultaKardex {

    private final MovimientoInventarioRepositoryPort movimientos;

    @Override
    public List<MovimientoKardex> consultarMovimientos(UUID nodoId, UUID varianteId, Instant desde, Instant hasta) {
        Objects.requireNonNull(nodoId, "nodoId es obligatorio");
        Objects.requireNonNull(varianteId, "varianteId es obligatorio");
        return movimientos.buscarPorNodoYVariante(nodoId, varianteId, desde, hasta);
    }
}
