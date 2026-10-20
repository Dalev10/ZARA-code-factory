package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;

import java.util.Optional;
import java.util.UUID;

/**
 * Dispara el traspaso automático para una variante en una tienda cuando el stock
 * del Almacén cae por debajo del umbral.
 *
 * Módulo: reposicion.
 */
public interface TraspasoAutomaticoUseCase {

    /**
     * Evalúa si el stock actual del Almacén exige reposición y, en ese caso, calcula
     * y persiste el traspaso, aplicando el movimiento contable en inventario.
     *
     * @param tiendaId   tienda donde evaluar
     * @param varianteId variante que disparó el evento de existencias
     * @return el traspaso ejecutado, o vacío si no se cumplió la condición de disparo
     *         o no había umbral configurado para la variante
     */
    Optional<TraspasoInterno> evaluar(UUID tiendaId, UUID varianteId);
}
