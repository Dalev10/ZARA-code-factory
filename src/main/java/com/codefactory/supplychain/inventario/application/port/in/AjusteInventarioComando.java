package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.domain.model.MotivoAjuste;
import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.util.UUID;

/**
 * Ajuste de una variante ya cargada en un nodo. El autor sale del token (D-08).
 */
public record AjusteInventarioComando(
        UUID nodoId,
        String sku,
        int delta,
        MotivoAjuste motivo,
        String comentario,
        UsuarioActual usuario) {
}
