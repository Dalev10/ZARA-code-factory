package com.codefactory.supplychain.identity.application.port.in;

import java.util.List;

/**
 * codigosRespaldo viaja en texto plano solo hasta el controlador — es la única vez
 * que el usuario los ve; en base de datos solo se guarda su hash.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record ConfirmarMfaResultado(List<String> codigosRespaldo) {
}
