package com.codefactory.supplychain.catalogo.application.port.out;

import com.codefactory.supplychain.catalogo.domain.model.Variante;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (Ports and Adapters) que define el contrato de
 * persistencia para {@link Variante}.
 * <p>
 * Esta interfaz pertenece a la capa de aplicación y es implementada por
 * un adaptador de infraestructura (por ejemplo, en
 * {@code infrastructure.adapter.out.persistence}). El dominio y la
 * aplicación no conocen ni dependen de la tecnología de persistencia
 * utilizada.
 */
public interface VarianteRepository {

    /**
     * Guarda una Variante. El mismo contrato sirve tanto para crear una
     * Variante nueva como para persistir cambios sobre una existente;
     * la decisión de cuál caso aplica depende del estado del objeto
     * recibido (por ejemplo, si ya tiene id asignado), no de este puerto.
     */
    Variante save(Variante variante);

    /**
     * Busca una Variante por su identificador.
     */
    Optional<Variante> findById(Long id);

    /**
     * Busca una Variante por su SKU.
     */
    Optional<Variante> findBySku(String sku);

    /**
     * Obtiene las Variantes existentes.
     */
    List<Variante> findAll();

    /**
     * Comprueba si existe una Variante con el identificador dado.
     */
    boolean existsById(Long id);

    /**
     * Comprueba si existe una Variante con el SKU dado.
     */
    boolean existsBySku(String sku);

    /**
     * Elimina una Variante por su identificador.
     * <p>
     * Este puerto no define si la eliminación es física o lógica; esa
     * decisión corresponde al adaptador de persistencia y se tomará al
     * implementar HU-18.
     */
    void deleteById(Long id);
}
