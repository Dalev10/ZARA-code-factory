package com.codefactory.supplychain.reposicion.domain.exception;

import java.util.UUID;

/**
 * Se lanza cuando el stock de la Bodega_Tienda no alcanza para ejecutar el traspaso
 * manual solicitado.
 *
 * Módulo: reposicion — Reposición interna.
 */
public class BodegaInsuficienteException extends RuntimeException {

    public BodegaInsuficienteException(UUID tiendaId, UUID varianteId,
                                        int disponible, int solicitado) {
        super(String.format(
                "Stock insuficiente en la Bodega de la tienda %s para la variante %s: "
                + "disponible=%d, solicitado=%d",
                tiendaId, varianteId, disponible, solicitado));
    }
}
