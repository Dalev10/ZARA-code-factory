package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SamstevensTotpAdapterTest {

    private final SamstevensTotpAdapter adapter = new SamstevensTotpAdapter();

    @Test
    void generarSecretoDevuelveUnValorBase32NoVacioYDistintoCadaVez() {
        String secreto1 = adapter.generarSecreto();
        String secreto2 = adapter.generarSecreto();

        assertThat(secreto1).isNotBlank();
        assertThat(secreto1).matches("[A-Z2-7]+");
        assertThat(secreto1).isNotEqualTo(secreto2);
    }

    @Test
    void verificarCodigoAceptaElCodigoRealmenteVigenteParaElSecreto() throws Exception {
        String secreto = adapter.generarSecreto();
        String codigoValido = new DefaultCodeGenerator().generate(secreto,
                new SystemTimeProvider().getTime() / 30);

        assertThat(adapter.verificarCodigo(secreto, codigoValido)).isTrue();
    }

    @Test
    void verificarCodigoRechazaUnCodigoIncorrecto() {
        String secreto = adapter.generarSecreto();

        assertThat(adapter.verificarCodigo(secreto, "000000")).isFalse();
    }

    @Test
    void generarQrDataUriDevuelveUnaImagenPngEnBase64() {
        String secreto = adapter.generarSecreto();

        String dataUri = adapter.generarQrDataUri(secreto, "ana@ejemplo.com");

        assertThat(dataUri).startsWith("data:image/png;base64,");
        assertThat(dataUri.length()).isGreaterThan("data:image/png;base64,".length());
    }
}
