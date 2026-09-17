package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.NombreCompletoInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

    private static final Email EMAIL = Email.de("usuario@ejemplo.com");
    private static final PasswordHash HASH = PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv");

    @Test
    void crearAsignaEstadoActivoYSinIntentosFallidos() {
        Usuario usuario = Usuario.crear(EMAIL, "Ana Pérez", HASH);

        assertThat(usuario.getId()).isNotNull();
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getBloqueadoHasta()).isNull();
        assertThat(usuario.isMfaHabilitado()).isFalse();
        assertThat(usuario.getMfaSecretEncrypted()).isNull();
        assertThat(usuario.getCreadoEn()).isEqualTo(usuario.getActualizadoEn());
    }

    @Test
    void rechazaNombreCompletoVacio() {
        assertThatThrownBy(() -> Usuario.crear(EMAIL, "   ", HASH))
                .isInstanceOf(NombreCompletoInvalidoException.class);
    }

    @Test
    void rechazaNombreCompletoDemasiadoLargo() {
        assertThatThrownBy(() -> Usuario.crear(EMAIL, "a".repeat(151), HASH))
                .isInstanceOf(NombreCompletoInvalidoException.class);
    }

    @Test
    void dosUsuariosSonIgualesSoloSiTienenElMismoId() {
        UUID id = UUID.randomUUID();
        Instant ahora = Instant.now();
        Usuario usuario1 = Usuario.reconstruir(id, EMAIL, "Ana Pérez", HASH, EstadoUsuario.ACTIVO,
                0, null, false, null, null, ahora, ahora);
        Usuario usuario2 = Usuario.reconstruir(id, EMAIL, "Otro Nombre", HASH, EstadoUsuario.BLOQUEADO,
                3, ahora, true, "otro-secreto", null, ahora, ahora);

        assertThat(usuario1).isEqualTo(usuario2);
    }

    @Test
    void reconstruirPreservaElEstadoPersistido() {
        UUID id = UUID.randomUUID();
        Instant creado = Instant.parse("2026-01-01T00:00:00Z");
        Instant actualizado = Instant.parse("2026-02-01T00:00:00Z");
        Instant bloqueadoHasta = Instant.parse("2026-02-02T00:00:00Z");

        Usuario usuario = Usuario.reconstruir(id, EMAIL, "Ana Pérez", HASH, EstadoUsuario.BLOQUEADO,
                5, bloqueadoHasta, true, "secreto-cifrado", "GOOGLE", creado, actualizado);

        assertThat(usuario.getId()).isEqualTo(id);
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.BLOQUEADO);
        assertThat(usuario.getIntentosFallidos()).isEqualTo(5);
        assertThat(usuario.getBloqueadoHasta()).isEqualTo(bloqueadoHasta);
        assertThat(usuario.isMfaHabilitado()).isTrue();
        assertThat(usuario.getMfaSecretEncrypted()).isEqualTo("secreto-cifrado");
        assertThat(usuario.getProveedorExterno()).isEqualTo("GOOGLE");
    }
}
