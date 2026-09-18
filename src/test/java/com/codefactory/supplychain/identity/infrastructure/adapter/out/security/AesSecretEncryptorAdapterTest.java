package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AesSecretEncryptorAdapterTest {

    private static final String CLAVE_DE_PRUEBA = generarClaveDePrueba();

    private final AesSecretEncryptorAdapter adapter = new AesSecretEncryptorAdapter(CLAVE_DE_PRUEBA);

    @Test
    void loQueSeEncriptaSeDesencriptaAlValorOriginal() {
        String cifrado = adapter.encriptar("JBSWY3DPEHPK3PXP");

        assertThat(adapter.desencriptar(cifrado)).isEqualTo("JBSWY3DPEHPK3PXP");
    }

    @Test
    void dosCifradosDelMismoValorSonDistintos() {
        String cifrado1 = adapter.encriptar("mismo-secreto");
        String cifrado2 = adapter.encriptar("mismo-secreto");

        // El IV aleatorio en cada llamada garantiza que el texto cifrado nunca se repita,
        // aunque el valor en claro y la clave sean los mismos.
        assertThat(cifrado1).isNotEqualTo(cifrado2);
        assertThat(adapter.desencriptar(cifrado1)).isEqualTo("mismo-secreto");
        assertThat(adapter.desencriptar(cifrado2)).isEqualTo("mismo-secreto");
    }

    @Test
    void elValorCifradoNoContieneElTextoPlanoEnClaro() {
        String cifrado = adapter.encriptar("secreto-super-sensible");

        assertThat(cifrado).doesNotContain("secreto-super-sensible");
    }

    private static String generarClaveDePrueba() {
        byte[] clave = new byte[32];
        new SecureRandom().nextBytes(clave);
        return Base64.getEncoder().encodeToString(clave);
    }
}
