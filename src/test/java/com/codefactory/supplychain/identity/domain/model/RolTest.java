package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.RolInvalidoException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RolTest {

    @Test
    void crearAsignaIdYFechaDeCreacion() {
        Rol rol = Rol.crear("ADMIN", "Administrador del sistema");

        assertThat(rol.getId()).isNotNull();
        assertThat(rol.getNombre()).isEqualTo("ADMIN");
        assertThat(rol.getCreadoEn()).isNotNull();
    }

    @Test
    void permiteDescripcionNula() {
        Rol rol = Rol.crear("ADMIN", null);

        assertThat(rol.getDescripcion()).isNull();
    }

    @Test
    void rechazaNombreVacio() {
        assertThatThrownBy(() -> Rol.crear("  ", "desc")).isInstanceOf(RolInvalidoException.class);
    }

    @Test
    void rechazaNombreDemasiadoLargo() {
        assertThatThrownBy(() -> Rol.crear("a".repeat(101), "desc")).isInstanceOf(RolInvalidoException.class);
    }

    @Test
    void rechazaDescripcionDemasiadoLarga() {
        assertThatThrownBy(() -> Rol.crear("ADMIN", "a".repeat(256))).isInstanceOf(RolInvalidoException.class);
    }
}
