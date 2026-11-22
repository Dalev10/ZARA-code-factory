package com.codefactory.supplychain.logistica.application.port.in;

import com.codefactory.supplychain.logistica.domain.model.Viaje;

import java.util.List;
import java.util.UUID;

/** Arma un viaje desde un CD con una parada por ciclo de despacho (HU-47). */
public interface ArmarViajeUseCase {

    Viaje armar(Comando comando);

    /** @param ordenParada nulo para usar la posición en la lista */
    record ParadaPedida(UUID cicloDespachoId, Integer ordenParada) {
    }

    record Comando(UUID cdId, String vehiculo, List<ParadaPedida> paradas) {
    }
}
