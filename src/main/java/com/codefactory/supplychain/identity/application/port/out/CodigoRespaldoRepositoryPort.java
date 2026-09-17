package com.codefactory.supplychain.identity.application.port.out;

import com.codefactory.supplychain.identity.domain.model.CodigoRespaldo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface CodigoRespaldoRepositoryPort {

    List<CodigoRespaldo> guardarTodos(List<CodigoRespaldo> codigos);

    CodigoRespaldo guardar(CodigoRespaldo codigo);

    Optional<CodigoRespaldo> buscarNoUsadoPorHash(UUID usuarioId, String codigoHash);
}
