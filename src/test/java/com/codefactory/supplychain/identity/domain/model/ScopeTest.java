package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.ScopeInvalidoException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScopeTest {

    @Test
    void crearAsignaIdYFechaDeCreacion() {
        Scope scope = Scope.crear("usuarios:escribir", "Permite crear y modificar usuarios", true);

        assertThat(scope.getId()).isNotNull();
        assertThat(scope.getCodigo()).isEqualTo("usuarios:escribir");
        assertThat(scope.isSensible()).isTrue();
        assertThat(scope.getCreadoEn()).isNotNull();
    }

    @Test
    void rechazaCodigoVacio() {
        assertThatThrownBy(() -> Scope.crear("  ", "desc", false)).isInstanceOf(ScopeInvalidoException.class);
    }

    @Test
    void rechazaCodigoDemasiadoLargo() {
        assertThatThrownBy(() -> Scope.crear("a".repeat(101), "desc", false))
                .isInstanceOf(ScopeInvalidoException.class);
    }

    @Test
    void rechazaDescripcionDemasiadoLarga() {
        assertThatThrownBy(() -> Scope.crear("codigo", "a".repeat(256), false))
                .isInstanceOf(ScopeInvalidoException.class);
    }
}
