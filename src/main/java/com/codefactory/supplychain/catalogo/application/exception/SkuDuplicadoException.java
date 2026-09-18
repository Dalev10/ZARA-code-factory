package com.codefactory.supplychain.catalogo.application.exception;

/**
 * Se lanza al intentar crear una Variante con un SKU que ya pertenece a
 * otra Variante existente, o al modificar una Variante asignándole un SKU
 * que ya usa otra Variante distinta de ella misma.
 * <p>
 * Ver la nota sobre convenciones de excepciones en
 * {@link RecursoNoEncontradoException}: no fue posible revisar
 * {@code shared/exception} porque no forma parte del ZIP de esta etapa.
 */
public class SkuDuplicadoException extends RuntimeException {

    public SkuDuplicadoException(String sku) {
        super("Ya existe una Variante con sku " + sku);
    }
}
