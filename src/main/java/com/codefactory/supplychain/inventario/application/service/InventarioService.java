package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.InventarioYaCargadoException;
import com.codefactory.supplychain.inventario.domain.exception.VarianteNoCargadaException;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Function;

/**
 * Orquesta operaciones sobre un Inventario existente.
 *
 * La búsqueda previa es deliberada: el agregado nunca se crea implícitamente
 * durante una operación, porque la carga inicial pertenece a un caso de uso
 * explícito.
 */
@Service
@RequiredArgsConstructor
public class InventarioService implements InventarioUseCase {

    private final InventarioRepositoryPort inventarioRepositoryPort;

    @Override
    public MovimientoInventario cargar(UUID nodoId, UUID varianteId, int cantidad) {
        if (inventarioRepositoryPort.buscarPorNodoYVariante(nodoId, varianteId).isPresent()) {
            throw new InventarioYaCargadoException(nodoId, varianteId);
        }
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        MovimientoInventario movimiento = inventario.cargar(cantidad);
        inventarioRepositoryPort.guardar(inventario);
        return movimiento;
    }

    @Override
    public MovimientoInventario reponer(UUID nodoId, UUID varianteId, int cantidad) {
        return ejecutar(nodoId, varianteId, inventario -> inventario.reponer(cantidad));
    }

    @Override
    public MovimientoInventario aplicarVenta(
            UUID nodoId,
            UUID varianteId,
            int cantidad,
            boolean forzado) {
        return ejecutar(nodoId, varianteId, inventario -> inventario.aplicarVenta(cantidad, forzado));
    }

    @Override
    public MovimientoInventario aplicarAjuste(
            UUID nodoId,
            UUID varianteId,
            int delta,
            String motivo) {
        return ejecutar(nodoId, varianteId, inventario -> inventario.aplicarAjuste(delta, motivo));
    }

    @Override
    public MovimientoInventario revertir(
            UUID nodoId,
            UUID varianteId,
            int cantidad,
            String motivo) {
        return ejecutar(nodoId, varianteId, inventario -> inventario.revertir(cantidad, motivo));
    }

    private MovimientoInventario ejecutar(
            UUID nodoId,
            UUID varianteId,
            Function<Inventario, MovimientoInventario> operacion) {
        Inventario inventario = inventarioRepositoryPort.buscarPorNodoYVariante(nodoId, varianteId)
                .orElseThrow(() -> new VarianteNoCargadaException(nodoId, varianteId));
        MovimientoInventario movimiento = operacion.apply(inventario);
        inventarioRepositoryPort.guardar(inventario);
        return movimiento;
    }
}
