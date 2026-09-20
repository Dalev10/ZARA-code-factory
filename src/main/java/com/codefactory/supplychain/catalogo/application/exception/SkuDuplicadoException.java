package com.codefactory.supplychain.catalogo.application.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

/**
 * Se lanza al intentar crear una Variante con un SKU que ya pertenece a
 * otra Variante existente, o al modificar una Variante asignándole un SKU
 * que ya usa otra Variante distinta de ella misma.
 *
 * Módulo: catalogo — Categoria/Template/Variante (FEAT-05).
 */
public class SkuDuplicadoException extends RecursoDuplicadoException {

    public SkuDuplicadoException(String sku) {
        super("Ya existe una Variante con sku " + sku);
    }
}
