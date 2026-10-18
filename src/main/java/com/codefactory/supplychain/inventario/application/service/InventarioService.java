package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoMovimientoKardex;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.MovimientoInventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.InventarioYaCargadoException;
import com.codefactory.supplychain.inventario.domain.exception.NodoNoEncontradoException;
import com.codefactory.supplychain.inventario.domain.exception.VarianteNoCargadaException;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class InventarioService {

    private final InventarioRepositoryPort inventarioRepositoryPort;
    private final MovimientoInventarioRepositoryPort movimientoInventarioRepositoryPort;
    private final NodoRepositoryPort nodoRepositoryPort;
    private final ApplicationEventPublisher eventos;

    @Transactional
    public MovimientoInventario cargar(UUID nodoId, UUID varianteId, int cantidad) {
        Nodo nodo = buscarNodo(nodoId);
        if (inventarioRepositoryPort.buscarPorNodoYVariante(nodoId, varianteId).isPresent()) {
            throw new InventarioYaCargadoException(nodoId, varianteId);
        }
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        MovimientoInventario movimiento = inventario.cargar(cantidad);
        inventarioRepositoryPort.guardar(inventario);
        registrarMovimiento(nodo, inventario, movimiento);
        return movimiento;
    }

    @Transactional
    public MovimientoInventario reponer(UUID nodoId, UUID varianteId, int cantidad) {
        return ejecutar(nodoId, varianteId, inventario -> inventario.reponer(cantidad));
    }

    @Transactional
    public MovimientoInventario aplicarVenta(
            UUID nodoId,
            UUID varianteId,
            int cantidad,
            boolean forzado) {
        return ejecutar(nodoId, varianteId, inventario -> inventario.aplicarVenta(cantidad, forzado));
    }

    @Transactional
    public MovimientoInventario aplicarAjuste(
            UUID nodoId,
            UUID varianteId,
            int delta,
            String motivo) {
        return ejecutar(nodoId, varianteId, inventario -> inventario.aplicarAjuste(delta, motivo));
    }

    @Transactional
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
        Nodo nodo = buscarNodo(nodoId);
        MovimientoInventario movimiento = operacion.apply(inventario);
        inventarioRepositoryPort.guardar(inventario);
        registrarMovimiento(nodo, inventario, movimiento);
        return movimiento;
    }

    private Nodo buscarNodo(UUID nodoId) {
        return nodoRepositoryPort.buscarPorId(nodoId)
                .orElseThrow(() -> new NodoNoEncontradoException(nodoId));
    }

    private void registrarMovimiento(
            Nodo nodo,
            Inventario inventario,
            MovimientoInventario movimiento) {

        movimientoInventarioRepositoryPort.guardar(
                inventario.getNodoId(),
                inventario.getVarianteId(),
                movimiento);
        publicarExistenciasActualizadas(nodo, inventario, movimiento);
    }
    
    /**
     * Utiliza el ApplicationEventPublisher de Spring para publicar un evento de existencias actualizadas 
     * después de registrar un movimiento de inventario.
     * 
     * Debe ser escuchado con @TransactionalEventListener para garantizar que el evento se publique 
     * solo después de que la transacción se haya completado con éxito.
     */
    private void publicarExistenciasActualizadas(
            Nodo nodo,
            Inventario inventario,
            MovimientoInventario movimiento) {
        eventos.publishEvent(new ExistenciasActualizadas(
                inventario.getNodoId(),
                TipoNodoInventario.valueOf(nodo.getTipo().name()),
                nodo.getTiendaId(),
                inventario.getVarianteId(),
                movimiento.id(),
                TipoMovimientoKardex.valueOf(movimiento.tipo().name()),
                movimiento.forzado(),
                null,
                inventario.getALaMano() - movimiento.cantidad(),
                inventario.getALaMano(),
                inventario.getDisponible(),
                inventario.getEntrante(),
                inventario.getSaliente(),
                inventario.getPronosticado(),
                movimiento.ocurridoEn()));
    }
}
