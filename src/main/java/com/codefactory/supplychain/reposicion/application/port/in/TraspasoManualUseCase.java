package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;

import java.util.UUID;

/**
 * Registra un traspaso manual de Bodega_Tienda → Almacén ejecutado físicamente
 * por un operario.
 * <p>
 * Diferencias respecto al automático:
 * <ul>
 *   <li>El origen siempre es MANUAL.</li>
 *   <li>{@code registradoPor} es obligatorio: viene del token JWT del operario.</li>
 *   <li>La cantidad la decide el operario, no el algoritmo; pero la BD y el
 *       dominio validan que no deje la Bodega en negativo.</li>
 * </ul>
 *
 * Módulo: reposicion — Reposición interna.
 */
public interface TraspasoManualUseCase {

    /**
     * @param tiendaId      tienda donde se ejecuta el traspaso
     * @param varianteId    variante que se mueve
     * @param cantidad      unidades a traspasar (> 0)
     * @param registradoPor id del usuario que registra (del token, nunca del body)
     * @throws com.codefactory.supplychain.reposicion.domain.exception.TraspasoInvalidoException
     *         si cantidad ≤ 0 o registradoPor es null
     * @throws com.codefactory.supplychain.reposicion.application.exception.BodegaInsuficienteException
     *         si la Bodega no tiene suficiente stock para el traspaso solicitado
     */
    TraspasoInterno registrar(UUID tiendaId, UUID varianteId, int cantidad, UUID registradoPor);
}
