package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.EmailInvalidoException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void normalizaAMinusculasYRecortaEspacios() {
        Email email = Email.de("  Usuario@Ejemplo.COM  ");

        assertThat(email.getValor()).isEqualTo("usuario@ejemplo.com");
    }

    @Test
    void dosEmailsConDistintoCasingSonIguales() {
        assertThat(Email.de("Usuario@Ejemplo.com")).isEqualTo(Email.de("usuario@ejemplo.com"));
    }

    @Test
    void rechazaValorVacio() {
        assertThatThrownBy(() -> Email.de("   ")).isInstanceOf(EmailInvalidoException.class);
    }

    @Test
    void rechazaFormatoInvalido() {
        assertThatThrownBy(() -> Email.de("no-es-un-email")).isInstanceOf(EmailInvalidoException.class);
    }

    @Test
    void rechazaValorDemasiadoLargo() {
        String local = "a".repeat(250);
        assertThatThrownBy(() -> Email.de(local + "@ejemplo.com")).isInstanceOf(EmailInvalidoException.class);
    }
}
