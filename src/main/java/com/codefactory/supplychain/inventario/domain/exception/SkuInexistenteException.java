package com.codefactory.supplychain.inventario.domain.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * El SKU indicado no existe en el catálogo.
 */
public class SkuInexistenteException extends ReglaDeNegocioException {

    public SkuInexistenteException(String sku) {
        super("El SKU " + sku + " no existe en el catálogo");
    }
}
