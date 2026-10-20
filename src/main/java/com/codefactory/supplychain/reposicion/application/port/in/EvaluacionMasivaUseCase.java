package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;

import java.util.List;
import java.util.UUID;

/**
 * HU-50: evalúa la reposición interna de una o varias tiendas de golpe.
 * Para cada tienda activa con umbral configurado, aplica la misma lógica que el
 * traspaso automático individual.
 */
public interface EvaluacionMasivaUseCase {

    /**
     * Evalúa todas las tiendas activas que tengan al menos un umbral configurado.
     *
     * @return traspasos ejecutados (vacíos = todo en umbral o sin stock en bodega).
     */
    List<TraspasoInterno> evaluarTodas();

    /**
     * Evalúa solo la tienda indicada.
     */
    List<TraspasoInterno> evaluarTienda(UUID tiendaId);
}
