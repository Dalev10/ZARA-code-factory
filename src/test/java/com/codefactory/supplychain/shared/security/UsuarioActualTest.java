package com.codefactory.supplychain.shared.security;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioActualTest {

    @Test
    void tieneScopeSoloParaLosScopesQueRealmentePosee() {
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), Set.of("inventario:administrar"));

        assertThat(usuario.tieneScope("inventario:administrar")).isTrue();
        assertThat(usuario.tieneScope("inventario-cd:administrar")).isFalse();
    }

    @Test
    void losScopesNoCambianSiSeModificaElConjuntoOriginal() {
        Set<String> original = new HashSet<>(Set.of("ventas:registrar"));
        UsuarioActual usuario = new UsuarioActual(UUID.randomUUID(), original);

        original.add("ventas:anular");

        assertThat(usuario.tieneScope("ventas:anular")).isFalse();
    }
}
