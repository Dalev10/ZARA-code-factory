package com.codefactory.supplychain.shared.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioActualTest {

    private static final UUID TIENDA_ASIGNADA = UUID.randomUUID();
    private static final UUID OTRA_TIENDA = UUID.randomUUID();

    @Test
    void tieneScopeSoloParaLosScopesQueRealmentePosee() {
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), Set.of("inventario:administrar"), Set.of());

        assertThat(usuario.tieneScope("inventario:administrar")).isTrue();
        assertThat(usuario.tieneScope("inventario-cd:administrar")).isFalse();
    }

    @Test
    void puedeOperarSoloEnLasTiendasQueTieneAsignadas() {
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), Set.of(), Set.of(TIENDA_ASIGNADA));

        assertThat(usuario.puedeOperarEn(TIENDA_ASIGNADA)).isTrue();
        assertThat(usuario.puedeOperarEn(OTRA_TIENDA)).isFalse();
    }

    @Test
    void conElScopeDeAccesoGlobalPuedeOperarEnCualquierTiendaSinAsignaciones() {
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(),
                Set.of(UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS), Set.of());

        assertThat(usuario.puedeOperarEn(OTRA_TIENDA)).isTrue();
    }

    @Test
    void losScopesYLasTiendasNoCambianSiSeModificanLosConjuntosOriginales() {
        Set<String> scopes = new HashSet<>(Set.of("ventas:registrar"));
        Set<UUID> tiendas = new HashSet<>(Set.of(TIENDA_ASIGNADA));
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), scopes, tiendas);

        scopes.add("ventas:anular");
        tiendas.add(OTRA_TIENDA);

        assertThat(usuario.tieneScope("ventas:anular")).isFalse();
        assertThat(usuario.puedeOperarEn(OTRA_TIENDA)).isFalse();
    }
}
