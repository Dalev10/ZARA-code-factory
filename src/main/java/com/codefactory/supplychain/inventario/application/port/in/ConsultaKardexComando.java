package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.domain.model.TipoMovimientoInventario;
import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.time.Instant;
import java.util.UUID;

/**
 * Filtros del kardex de un nodo. {@code tipo}, {@code desde} y {@code hasta}
 * son opcionales: {@code null} no filtra por ese campo.
 */
public record ConsultaKardexComando(
        UUID nodoId,
        TipoMovimientoInventario tipo,
        Instant desde,
        Instant hasta,
        UsuarioActual usuario) {
}
