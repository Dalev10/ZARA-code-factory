package com.codefactory.supplychain.identity.application.port.out;

import com.codefactory.supplychain.identity.domain.model.CodigoRespaldo;

import java.util.List;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface CodigoRespaldoRepositoryPort {

    List<CodigoRespaldo> guardarTodos(List<CodigoRespaldo> codigos);
}
