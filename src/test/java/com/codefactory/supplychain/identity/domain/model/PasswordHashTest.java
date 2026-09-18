package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.PasswordHashInvalidoException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHashTest {

    @Test
    void aceptaCualquierHashNoVacioDentroDelLimite() {
        PasswordHash hash = PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv");

        assertThat(hash.getValor()).isEqualTo("$2a$10$abcdefghijklmnopqrstuv");
    }

    @Test
    void rechazaValorVacio() {
        assertThatThrownBy(() -> PasswordHash.de(" ")).isInstanceOf(PasswordHashInvalidoException.class);
    }

    @Test
    void rechazaValorDemasiadoLargo() {
        assertThatThrownBy(() -> PasswordHash.de("a".repeat(256)))
                .isInstanceOf(PasswordHashInvalidoException.class);
    }

    @Test
    void nuncaExponeElHashEnToString() {
        PasswordHash hash = PasswordHash.de("valor-secreto-que-no-debe-aparecer");

        assertThat(hash.toString()).doesNotContain("valor-secreto-que-no-debe-aparecer");
    }
}
