package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

import java.util.UUID;

/**
 * Ya existe un umbral para la variante o la categoría indicada (D-51: uno por alcance).
 */
public class UmbralYaExisteException extends RecursoDuplicadoException {

    private UmbralYaExisteException(String mensaje) {
        super(mensaje);
    }

    public static UmbralYaExisteException paraVariante(UUID varianteId) {
        return new UmbralYaExisteException("Ya existe un umbral de reposición para la variante " + varianteId);
    }

    public static UmbralYaExisteException paraCategoria(UUID categoriaId) {
        return new UmbralYaExisteException("Ya existe un umbral de reposición para la categoría " + categoriaId);
    }
}
