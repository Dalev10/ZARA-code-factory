package com.codefactory.supplychain.reposicion.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Lo que reposicion necesita saber de una variante, en su propio lenguaje: no menciona
 * a catalogo. Lo implementa el adaptador de {@code adapter/out/catalogo} (ACL).
 */
public interface VarianteConsultaPort {

    Optional<VarianteConsultada> buscar(UUID varianteId);

    /** Datos de la variante que usa reposicion: su id y la categoría a la que pertenece. */
    record VarianteConsultada(UUID id, UUID categoriaId) {
    }
}
