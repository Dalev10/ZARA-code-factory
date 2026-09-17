package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.PasswordDebilException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordTest {

    @Test
    void aceptaUnaContraseñaDeAlMenos12Caracteres() {
        Password password = Password.de("contraseñaSegura123");

        assertThat(password.getValor()).isEqualTo("contraseñaSegura123");
    }

    @Test
    void rechazaValorVacio() {
        assertThatThrownBy(() -> Password.de("   ")).isInstanceOf(PasswordDebilException.class);
    }

    @Test
    void rechazaMenosDe12Caracteres() {
        assertThatThrownBy(() -> Password.de("corta12345")).isInstanceOf(PasswordDebilException.class);
    }

    @Test
    void aceptaExactamente12Caracteres() {
        Password password = Password.de("a".repeat(12));

        assertThat(password.getValor()).hasSize(12);
    }

    @Test
    void rechazaMasDe128Caracteres() {
        assertThatThrownBy(() -> Password.de("a".repeat(129))).isInstanceOf(PasswordDebilException.class);
    }

    @Test
    void nuncaExponeLaContraseñaEnToString() {
        Password password = Password.de("valor-secreto-que-no-debe-aparecer");

        assertThat(password.toString()).doesNotContain("valor-secreto-que-no-debe-aparecer");
    }
}
