package com.codefactory.supplychain.reposicion.domain.exception;

import com.codefactory.supplychain.shared.exception.RecursoDuplicadoException;

import java.util.UUID;

/**
 * Ya existe un nivel de reposición al CD para la variante o la categoría indicada (uno por alcance).
 */
public class NivelReposicionYaExisteException extends RecursoDuplicadoException {

    private NivelReposicionYaExisteException(String mensaje) {
        super(mensaje);
    }

    public static NivelReposicionYaExisteException paraVariante(UUID varianteId) {
        return new NivelReposicionYaExisteException(
                "Ya existe un nivel de reposición al CD para la variante " + varianteId);
    }

    public static NivelReposicionYaExisteException paraCategoria(UUID categoriaId) {
        return new NivelReposicionYaExisteException(
                "Ya existe un nivel de reposición al CD para la categoría " + categoriaId);
    }
}
