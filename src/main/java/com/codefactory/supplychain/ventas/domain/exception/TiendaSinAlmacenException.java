package com.codefactory.supplychain.ventas.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/** No se pudo resolver el Almacén de la tienda (HTTP 400). */
public class TiendaSinAlmacenException extends ReglaDeNegocioException {
    public TiendaSinAlmacenException(java.util.UUID tiendaId) {
        super("La tienda " + tiendaId + " no existe o no tiene un Almacén asociado");
    }
}
