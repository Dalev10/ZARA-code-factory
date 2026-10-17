package com.codefactory.supplychain.catalogo.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Consulta de variantes para otros módulos (primer consumidor: reposicion, HU-38).
 * La implementa un servicio de aplicación de catalogo, que traduce su dominio al
 * {@link VarianteResumen} publicado.
 */
public interface ConsultaVariantes {

    Optional<VarianteResumen> buscar(UUID varianteId);
}
