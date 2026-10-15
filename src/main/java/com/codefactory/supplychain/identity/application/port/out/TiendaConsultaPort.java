package com.codefactory.supplychain.identity.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que identity necesita saber de las tiendas, en su propio lenguaje. Lo implementa un
 * adaptador que consulta la API pública de inventario (capa anticorrupción): la capa de
 * aplicación de identity no conoce al módulo inventario.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface TiendaConsultaPort {

    Optional<TiendaAsignable> buscar(UUID tiendaId);

    List<TiendaAsignable> buscarTodas(Collection<UUID> tiendaIds);
}
