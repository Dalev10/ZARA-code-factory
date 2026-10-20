package com.codefactory.supplychain.ventas.application.service;

import com.codefactory.supplychain.shared.security.UsuarioActual;

import java.util.Collection;
import java.util.UUID;

/** Reglas de alcance por tienda compartidas por los servicios de ventas. */
final class AccesoTiendas {

    private AccesoTiendas() {
    }

    /** Nulo significa "sin restricción" (usuario con acceso a todas las tiendas). */
    static Collection<UUID> tiendasPermitidas(UsuarioActual usuario) {
        return usuario.tieneScope(UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS) ? null : usuario.tiendasPermitidas();
    }

    /** Un filtro por tienda ajena no puede devolver nada. */
    static boolean filtroFueraDeAlcance(UsuarioActual usuario, UUID tiendaId) {
        return tiendaId != null && !usuario.puedeOperarEn(tiendaId);
    }
}
