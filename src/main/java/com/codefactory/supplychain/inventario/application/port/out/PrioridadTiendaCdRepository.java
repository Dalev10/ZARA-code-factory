package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida de persistencia de {@link PrioridadTiendaCd}.
 */
public interface PrioridadTiendaCdRepository {

    List<PrioridadTiendaCd> listarPorTienda(UUID tiendaId);

    void guardarTodas(List<PrioridadTiendaCd> prioridades);

    void eliminarPorTienda(UUID tiendaId);

    boolean existePorCd(UUID cdId);
}
