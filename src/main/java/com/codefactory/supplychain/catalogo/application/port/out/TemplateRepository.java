package com.codefactory.supplychain.catalogo.application.port.out;

import java.util.UUID;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Puerto de salida (Ports and Adapters) que define el contrato de
 * persistencia para {@link Template}.
 * <p>
 * Esta interfaz pertenece a la capa de aplicación y es implementada por
 * un adaptador de infraestructura (por ejemplo, en
 * {@code infrastructure.adapter.out.persistence}). El dominio y la
 * aplicación no conocen ni dependen de la tecnología de persistencia
 * utilizada.
 */
public interface TemplateRepository {

    /**
     * Guarda un Template. El mismo contrato sirve tanto para crear un
     * Template nuevo como para persistir cambios sobre uno existente;
     * la decisión de cuál caso aplica depende del estado del objeto
     * recibido (por ejemplo, si ya tiene id asignado), no de este puerto.
     */
    Template save(Template template);

    /**
     * Busca un Template por su identificador.
     */
    Optional<Template> findById(UUID id);

    /**
     * Obtiene los Templates existentes, paginados.
     */
    Page<Template> findAll(Pageable pageable);

    /**
     * Comprueba si existe un Template con el identificador dado.
     */
    boolean existsById(UUID id);

    /**
     * Elimina un Template por su identificador.
     * <p>
     * Este puerto no define si la eliminación es física o lógica; esa
     * decisión corresponde al adaptador de persistencia y se tomará al
     * implementar HU-18.
     */
    void deleteById(UUID id);
}
