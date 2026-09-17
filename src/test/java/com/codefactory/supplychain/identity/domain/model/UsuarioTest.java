package com.codefactory.supplychain.identity.domain.model;

import com.codefactory.supplychain.identity.domain.exception.MfaNoConfiguradoException;
import com.codefactory.supplychain.identity.domain.exception.MfaYaActivoException;
import com.codefactory.supplychain.identity.domain.exception.NombreCompletoInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
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

    @Test
    void losPrimerosDosIntentosFallidosNoBloquean() {
        Usuario usuario = Usuario.crear(EMAIL, "Ana Pérez", HASH);
        Instant ahora = Instant.now();

        Usuario tras1 = usuario.registrarIntentoFallido(ahora);
        assertThat(tras1.getIntentosFallidos()).isEqualTo(1);
        assertThat(tras1.getBloqueadoHasta()).isNull();

        Usuario tras2 = tras1.registrarIntentoFallido(ahora);
        assertThat(tras2.getIntentosFallidos()).isEqualTo(2);
        assertThat(tras2.getBloqueadoHasta()).isNull();
    }

    @Test
    void alTercerIntentoFallidoBloqueaUnMinuto() {
        Usuario usuario = usuarioConIntentosFallidos(2);
        Instant ahora = Instant.now();

        Usuario tras3 = usuario.registrarIntentoFallido(ahora);

        assertThat(tras3.getIntentosFallidos()).isEqualTo(3);
        assertThat(tras3.getBloqueadoHasta()).isEqualTo(ahora.plus(Duration.ofMinutes(1)));
        assertThat(tras3.estaBloqueadoTemporalmente(ahora)).isTrue();
    }

    @Test
    void alQuintoIntentoFallidoBloqueaCincoMinutos() {
        Usuario usuario = usuarioConIntentosFallidos(4);
        Instant ahora = Instant.now();

        Usuario tras5 = usuario.registrarIntentoFallido(ahora);

        assertThat(tras5.getBloqueadoHasta()).isEqualTo(ahora.plus(Duration.ofMinutes(5)));
    }

    @Test
    void alSeptimoIntentoFallidoBloqueaQuinceMinutos() {
        Usuario usuario = usuarioConIntentosFallidos(6);
        Instant ahora = Instant.now();

        Usuario tras7 = usuario.registrarIntentoFallido(ahora);

        assertThat(tras7.getBloqueadoHasta()).isEqualTo(ahora.plus(Duration.ofMinutes(15)));
    }

    @Test
    void desdeElDecimoIntentoFallidoElBloqueoEsDeUnaHoraYNoSigueEscalando() {
        Usuario usuario = usuarioConIntentosFallidos(9);
        Instant ahora = Instant.now();

        Usuario tras10 = usuario.registrarIntentoFallido(ahora);
        assertThat(tras10.getBloqueadoHasta()).isEqualTo(ahora.plus(Duration.ofHours(1)));

        Usuario tras20 = usuarioConIntentosFallidos(19).registrarIntentoFallido(ahora);
        assertThat(tras20.getBloqueadoHasta()).isEqualTo(ahora.plus(Duration.ofHours(1)));
    }

    @Test
    void loginExitosoReseteaIntentosFallidosYLimpiaElBloqueo() {
        Usuario usuario = usuarioConIntentosFallidos(9).registrarIntentoFallido(Instant.now());
        Instant ahora = Instant.now();

        Usuario tras = usuario.registrarLoginExitoso(ahora);

        assertThat(tras.getIntentosFallidos()).isZero();
        assertThat(tras.getBloqueadoHasta()).isNull();
        assertThat(tras.estaBloqueadoTemporalmente(ahora)).isFalse();
    }

    @Test
    void estaBloqueadoTemporalmenteEsFalsoUnaVezPasadaLaFechaDeBloqueo() {
        Instant ahora = Instant.now();
        Usuario usuario = Usuario.reconstruir(UUID.randomUUID(), EMAIL, "Ana Pérez", HASH, EstadoUsuario.ACTIVO,
                3, ahora.minusSeconds(1), false, null, null, ahora, ahora);

        assertThat(usuario.estaBloqueadoTemporalmente(ahora)).isFalse();
    }

    private static Usuario usuarioConIntentosFallidos(int intentos) {
        Instant ahora = Instant.now();
        return Usuario.reconstruir(UUID.randomUUID(), EMAIL, "Ana Pérez", HASH, EstadoUsuario.ACTIVO,
                intentos, null, false, null, null, ahora, ahora);
    }

    @Test
    void iniciarActivacionMfaGuardaElSecretoCifradoSinHabilitarMfaTodavia() {
        Usuario usuario = Usuario.crear(EMAIL, "Ana Pérez", HASH);
        Instant ahora = Instant.now();

        Usuario actualizado = usuario.iniciarActivacionMfa("secreto-cifrado", ahora);

        assertThat(actualizado.getMfaSecretEncrypted()).isEqualTo("secreto-cifrado");
        assertThat(actualizado.isMfaHabilitado()).isFalse();
    }

    @Test
    void noSePuedeIniciarActivacionMfaSiYaEstaHabilitado() {
        Usuario usuario = Usuario.reconstruir(UUID.randomUUID(), EMAIL, "Ana Pérez", HASH, EstadoUsuario.ACTIVO,
                0, null, true, "secreto-ya-confirmado", null, Instant.now(), Instant.now());

        assertThatThrownBy(() -> usuario.iniciarActivacionMfa("otro-secreto", Instant.now()))
                .isInstanceOf(MfaYaActivoException.class);
    }

    @Test
    void confirmarActivacionMfaHabilitaMfaConservandoElSecreto() {
        Usuario usuario = Usuario.crear(EMAIL, "Ana Pérez", HASH)
                .iniciarActivacionMfa("secreto-cifrado", Instant.now());

        Usuario confirmado = usuario.confirmarActivacionMfa(Instant.now());

        assertThat(confirmado.isMfaHabilitado()).isTrue();
        assertThat(confirmado.getMfaSecretEncrypted()).isEqualTo("secreto-cifrado");
    }

    @Test
    void noSePuedeConfirmarActivacionMfaSinHaberlaIniciadoAntes() {
        Usuario usuario = Usuario.crear(EMAIL, "Ana Pérez", HASH);

        assertThatThrownBy(() -> usuario.confirmarActivacionMfa(Instant.now()))
                .isInstanceOf(MfaNoConfiguradoException.class);
    }
}
