package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.util.List;
import java.util.UUID;

/**
 * Lote de carga inicial de un nodo. El usuario sale del token (D-08).
 */
public record CargaInicialComando(UUID nodoId, List<ItemCargaInicial> items, UsuarioActual usuario) {

    public CargaInicialComando {
        items = List.copyOf(items);
    }
}
