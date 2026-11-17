package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import com.codefactory.supplychain.inventario.application.port.in.ReservarInventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.in.ResultadoReservaCd;
import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.NodoRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.BodegaTiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.exception.CentroDistribucionNoEncontradoException;
import com.codefactory.supplychain.inventario.domain.exception.InventarioInvalidoException;
import com.codefactory.supplychain.inventario.domain.model.Inventario;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Reserva y liberación de existencias de un CD para una Bodega_Tienda (HD-01).
 *
 * <ul>
 *   <li>Las filas se acceden en el orden CD → Bodega_Tienda (D-106).</li>
 *   <li>Sin movimiento de kardex (D-93): cada fila cambiada publica
 *       {@link ExistenciasActualizadas} con el movimiento nulo (D-100).</li>
 *   <li>Sin {@code @Retryable}: se une a la transacción del llamador, que
 *       reintenta fuera de ella (D-24).</li>
 *   <li>Si la variante nunca estuvo en la Bodega_Tienda, la fila se crea al
 *       sumar el entrante (filas por demanda, D-25 / D-104).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ReservaInventarioService implements ReservarInventarioUseCase {

    private final InventarioRepositoryPort inventarioRepositoryPort;
    private final NodoRepositoryPort nodoRepositoryPort;
    private final ApplicationEventPublisher eventos;

    @Override
    @Transactional
    public ResultadoReservaCd reservar(UUID cdId, UUID tiendaId, UUID varianteId, int cantidad, UUID referenciaId) {
        if (cantidad <= 0) {
            throw new InventarioInvalidoException("La cantidad a reservar debe ser mayor que cero");
        }
        Nodo nodoCd = buscarNodoCd(cdId);
        Nodo bodega = buscarBodega(tiendaId);
        Optional<Inventario> filaCd = inventarioRepositoryPort.buscarPorNodoYVariante(nodoCd.getId(), varianteId);
        int disponibleCd = filaCd.map(Inventario::getDisponible).orElse(0);
        if (filaCd.isEmpty() || !filaCd.get().cubre(cantidad)) {
            return new ResultadoReservaCd(false, disponibleCd);
        }

        Inventario inventarioCd = filaCd.get();
        inventarioCd.reservarSalida(cantidad);
        guardarYPublicar(nodoCd, inventarioCd, referenciaId);

        Inventario inventarioBodega = buscarOCrear(bodega, varianteId);
        inventarioBodega.sumarEntrante(cantidad);
        guardarYPublicar(bodega, inventarioBodega, referenciaId);

        return new ResultadoReservaCd(true, inventarioCd.getDisponible());
    }

    @Override
    @Transactional
    public void liberar(UUID cdId, UUID tiendaId, UUID varianteId, int reservaALiberar, int entranteALiberar,
                        UUID referenciaId) {
        Nodo nodoCd = buscarNodoCd(cdId);
        Nodo bodega = buscarBodega(tiendaId);
        if (reservaALiberar > 0) {
            inventarioRepositoryPort.buscarPorNodoYVariante(nodoCd.getId(), varianteId).ifPresent(inventario -> {
                inventario.liberarSalida(reservaALiberar);
                guardarYPublicar(nodoCd, inventario, referenciaId);
            });
        }
        if (entranteALiberar > 0) {
            inventarioRepositoryPort.buscarPorNodoYVariante(bodega.getId(), varianteId).ifPresent(inventario -> {
                inventario.liberarEntrante(entranteALiberar);
                guardarYPublicar(bodega, inventario, referenciaId);
            });
        }
    }

    private Nodo buscarNodoCd(UUID cdId) {
        return nodoRepositoryPort.buscarPorCdId(cdId)
                .orElseThrow(() -> new CentroDistribucionNoEncontradoException("El CD " + cdId + " no existe"));
    }

    private Nodo buscarBodega(UUID tiendaId) {
        return nodoRepositoryPort.buscarPorTiendaYTipo(tiendaId, TipoNodo.BODEGA_TIENDA)
                .orElseThrow(BodegaTiendaNoEncontradaException::new);
    }

    private Inventario buscarOCrear(Nodo nodo, UUID varianteId) {
        return inventarioRepositoryPort.buscarPorNodoYVariante(nodo.getId(), varianteId)
                .orElseGet(() -> Inventario.crear(nodo.getId(), varianteId));
    }

    private void guardarYPublicar(Nodo nodo, Inventario inventario, UUID referenciaId) {
        inventarioRepositoryPort.guardar(inventario);
        eventos.publishEvent(new ExistenciasActualizadas(
                inventario.getNodoId(),
                TipoNodoInventario.valueOf(nodo.getTipo().name()),
                nodo.getTiendaId(),
                inventario.getVarianteId(),
                null,
                null,
                false,
                referenciaId,
                inventario.getALaMano(),
                inventario.getALaMano(),
                inventario.getDisponible(),
                inventario.getEntrante(),
                inventario.getSaliente(),
                inventario.getPronosticado(),
                Instant.now()));
    }
}
