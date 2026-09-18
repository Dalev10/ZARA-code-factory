package com.codefactory.supplychain.catalogo.application.port.out;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (Ports and Adapters) que define el contrato de
 * persistencia para {@link Categoria}.
 * <p>
 * Esta interfaz pertenece a la capa de aplicación y es implementada por
 * un adaptador de infraestructura (por ejemplo, en
 * {@code infrastructure.adapter.out.persistence}). El dominio y la
 * aplicación no conocen ni dependen de la tecnología de persistencia
 * utilizada.
 */
public interface CategoriaRepository {

    /**
     * Guarda una Categoria. El mismo contrato sirve tanto para crear una
     * Categoria nueva como para persistir cambios sobre una existente;
     * la decisión de cuál caso aplica depende del estado del objeto
     * recibido (por ejemplo, si ya tiene id asignado), no de este puerto.
     */
    Categoria save(Categoria categoria);

    /**
     * Busca una Categoria por su identificador.
     */
    Optional<Categoria> findById(Long id);

    /**
     * Obtiene las Categorias existentes.
     */
    List<Categoria> findAll();

    /**
     * Comprueba si existe una Categoria con el identificador dado.
     */
    boolean existsById(Long id);

    /**
     * Elimina una Categoria por su identificador.
     * <p>
     * Este puerto no define si la eliminación es física o lógica; esa
     * decisión corresponde al adaptador de persistencia y se tomará al
     * implementar HU-18.
     */
    void deleteById(Long id);
}
