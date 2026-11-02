package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.util.List;
import java.util.UUID;

/**
 * HU-50: evalúa la reposición interna de una o varias tiendas de golpe.
 * Para cada tienda activa con umbral configurado, aplica la misma lógica que el
 * traspaso automático individual.
 */
public interface EvaluacionMasivaUseCase {

    /**
     * Evalúa las tiendas activas en las que el usuario puede operar (todas, con
     * tiendas:operar-todas).
     *
     * @return traspasos ejecutados (vacíos = todo en umbral o sin stock en bodega).
     */
    List<TraspasoInterno> evaluarTodas(UsuarioActual usuario);

    /**
     * Evalúa solo la tienda indicada; falla si el usuario no puede operar en ella.
     */
    List<TraspasoInterno> evaluarTienda(UsuarioActual usuario, UUID tiendaId);
}
