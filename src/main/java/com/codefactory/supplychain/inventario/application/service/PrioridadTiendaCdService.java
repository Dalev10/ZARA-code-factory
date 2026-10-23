package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.application.port.in.PrioridadTiendaCdUseCase;
import com.codefactory.supplychain.inventario.application.port.out.CentroDistribucionRepository;
import com.codefactory.supplychain.inventario.application.port.out.PrioridadTiendaCdRepository;
import com.codefactory.supplychain.inventario.application.port.out.TiendaRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.CdEnUsoPorPrioridadException;
import com.codefactory.supplychain.inventario.domain.exception.CentroDistribucionNoEncontradoException;
import com.codefactory.supplychain.inventario.domain.exception.TiendaNoEncontradaException;
import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gestión de prioridades Tienda-CD (HU-51).
 * <p>
 * El reemplazo borra todas las entradas de la tienda e inserta la nueva lista en la
 * misma transacción. El DEFERRABLE de V23 garantiza que Postgres solo verifica el
 * UNIQUE al commit y no en el DELETE intermedio.
 */
@Service
@Transactional(readOnly = true)
public class PrioridadTiendaCdService implements PrioridadTiendaCdUseCase {

    private final PrioridadTiendaCdRepository repositorio;
    private final TiendaRepositoryPort tiendas;
    private final CentroDistribucionRepository centros;

    public PrioridadTiendaCdService(PrioridadTiendaCdRepository repositorio, TiendaRepositoryPort tiendas,
                                    CentroDistribucionRepository centros) {
        this.repositorio = repositorio;
        this.tiendas = tiendas;
        this.centros = centros;
    }

    @Override
    @Transactional
    public List<PrioridadTiendaCd> reemplazar(UUID tiendaId, List<UUID> cdIds) {
        tiendas.buscarPorId(tiendaId).orElseThrow(TiendaNoEncontradaException::new);
        for (UUID cdId : cdIds) {
            centros.buscarPorId(cdId).orElseThrow(() -> new CentroDistribucionNoEncontradoException(
                    "No existe un centro de distribución con el id " + cdId));
        }

        repositorio.eliminarPorTienda(tiendaId);

        List<PrioridadTiendaCd> nuevas = new ArrayList<>();
        for (int i = 0; i < cdIds.size(); i++) {
            nuevas.add(PrioridadTiendaCd.crear(tiendaId, cdIds.get(i), i + 1));
        }
        repositorio.guardarTodas(nuevas);
        return nuevas;
    }

    @Override
    public List<PrioridadTiendaCd> listar(UUID tiendaId) {
        return repositorio.listarPorTienda(tiendaId);
    }

    @Override
    public void verificarQueNoEstaEnUso(UUID cdId) {
        if (repositorio.existePorCd(cdId)) {
            throw new CdEnUsoPorPrioridadException(cdId);
        }
    }
}
