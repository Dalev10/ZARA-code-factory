package com.codefactory.supplychain.catalogo.application.port.out;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida (Ports and Adapters) que define el contrato de
 * persistencia para {@link Categoria}.
 */
public interface CategoriaRepository {

    Categoria save(Categoria categoria);

    Optional<Categoria> findById(UUID id);

    List<Categoria> findAll();

    boolean existsById(UUID id);

    void deleteById(UUID id);
}
