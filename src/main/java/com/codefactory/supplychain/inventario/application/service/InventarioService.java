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
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.domain.model.MotivoAjuste;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Function;

/**
 * Orquesta las operaciones sobre el agregado Inventario.
 *
 * Filas por demanda (D-25): una variante sin fila en el nodo tiene stock 0 y
 * la fila se crea con la primera operación que la necesita. La carga inicial
 * es la excepción: solo crea filas nuevas (D-28).
 */
@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepositoryPort inventarioRepositoryPort;
    private final MovimientoInventarioRepositoryPort movimientoInventarioRepositoryPort;
    private final NodoRepositoryPort nodoRepositoryPort;
    private final ApplicationEventPublisher eventos;

    @Transactional
    public MovimientoInventario cargar(UUID nodoId, UUID varianteId, int cantidad, UUID usuarioId) {
        Nodo nodo = buscarNodo(nodoId);
        if (inventarioRepositoryPort.buscarPorNodoYVariante(nodoId, varianteId).isPresent()) {
            throw new InventarioYaCargadoException(nodoId, varianteId);
        }
        Inventario inventario = Inventario.crear(nodoId, varianteId);
        MovimientoInventario movimiento = inventario.cargar(cantidad, usuarioId);
        inventarioRepositoryPort.guardar(inventario);
        registrarMovimiento(nodo, inventario, movimiento);
        return movimiento;
    }

    @Transactional
    public MovimientoInventario aplicarVenta(
            UUID nodoId,
            UUID varianteId,
            int cantidad,
            boolean forzado,
            UUID ventaId) {
        return ejecutar(nodoId, varianteId, inventario -> inventario.aplicarVenta(cantidad, forzado, ventaId));
    }

    @Transactional
    public MovimientoInventario aplicarAjuste(
            UUID nodoId,
            UUID varianteId,
            int delta,
            MotivoAjuste motivo,
            String comentario,
            UUID usuarioId) {
        return ejecutar(nodoId, varianteId,
                inventario -> inventario.aplicarAjuste(delta, motivo, comentario, usuarioId));
    }

    @Transactional
    public MovimientoInventario revertir(UUID nodoId, UUID varianteId, int cantidad, UUID ventaId) {
        return ejecutar(nodoId, varianteId, inventario -> inventario.revertir(cantidad, ventaId));
    }

    /** Salida y entrada en la misma transacción; el origen se bloquea primero (D-24). */
    @Transactional
    public void trasladar(UUID nodoOrigenId, UUID nodoDestinoId, UUID varianteId, int cantidad, UUID traspasoId) {
        ejecutar(nodoOrigenId, varianteId, inventario -> inventario.despacharTraspaso(cantidad, traspasoId));
        ejecutar(nodoDestinoId, varianteId, inventario -> inventario.recibirTraspaso(cantidad, traspasoId));
    }

    private MovimientoInventario ejecutar(
            UUID nodoId,
            UUID varianteId,
            Function<Inventario, MovimientoInventario> operacion) {
        Nodo nodo = buscarNodo(nodoId);
        Inventario inventario = inventarioRepositoryPort.buscarPorNodoYVariante(nodoId, varianteId)
                .orElseGet(() -> Inventario.crear(nodoId, varianteId));
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
                movimiento.referenciaId(),
                inventario.getALaMano() - movimiento.cantidad(),
                inventario.getALaMano(),
                inventario.getDisponible(),
                inventario.getEntrante(),
                inventario.getSaliente(),
                inventario.getPronosticado(),
                movimiento.ocurridoEn()));
    }
}
