package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherAdapterTest {

    private final BCryptPasswordHasherAdapter adapter = new BCryptPasswordHasherAdapter(new BCryptPasswordEncoder());

    @Test
    void unaPasswordCoincideConSuPropioHash() {
        PasswordHash hash = adapter.hashear(Password.de("contraseñaSegura123"));

        assertThat(adapter.coincide("contraseñaSegura123", hash)).isTrue();
    }

    @Test
    void unaPasswordIncorrectaNoCoincide() {
        PasswordHash hash = adapter.hashear(Password.de("contraseñaSegura123"));

        assertThat(adapter.coincide("otraContraseña123", hash)).isFalse();
    }

    @Test
    void dosHasheosDeLaMismaPasswordSonDistintos() {
        PasswordHash hash1 = adapter.hashear(Password.de("contraseñaSegura123"));
        PasswordHash hash2 = adapter.hashear(Password.de("contraseñaSegura123"));

        assertThat(hash1.getValor()).isNotEqualTo(hash2.getValor());
    }
}
