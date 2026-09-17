package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.CompletarLoginMfaComando;
import com.codefactory.supplychain.identity.application.port.in.LoginResultado;
import com.codefactory.supplychain.identity.application.port.out.AccessTokenGeneratorPort;
import com.codefactory.supplychain.identity.application.port.out.CodigoRespaldoRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.MfaChallengeTokenPort;
import com.codefactory.supplychain.identity.application.port.out.RefreshTokenRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.SecretEncryptorPort;
import com.codefactory.supplychain.identity.application.port.out.TotpPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.CodigoMfaInvalidoException;
import com.codefactory.supplychain.identity.domain.exception.CredencialesInvalidasException;
import com.codefactory.supplychain.identity.domain.exception.TokenInvalidoException;
import com.codefactory.supplychain.identity.domain.model.CodigoRespaldo;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.RefreshToken;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompletarLoginMfaServiceTest {

    private static final PasswordHash HASH = PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv");

    private MfaChallengeTokenPort mfaChallengeTokenPort;
    private UsuarioRepositoryPort usuarioRepositoryPort;
    private TotpPort totpPort;
    private SecretEncryptorPort secretEncryptorPort;
    private CodigoRespaldoRepositoryPort codigoRespaldoRepositoryPort;
    private AccessTokenGeneratorPort accessTokenGeneratorPort;
    private RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private CompletarLoginMfaService servicio;

    @BeforeEach
    void setUp() {
        mfaChallengeTokenPort = mock(MfaChallengeTokenPort.class);
        usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        totpPort = mock(TotpPort.class);
        secretEncryptorPort = mock(SecretEncryptorPort.class);
        codigoRespaldoRepositoryPort = mock(CodigoRespaldoRepositoryPort.class);
        accessTokenGeneratorPort = mock(AccessTokenGeneratorPort.class);
        refreshTokenRepositoryPort = mock(RefreshTokenRepositoryPort.class);
        EmisionTokensService emisionTokensService = new EmisionTokensService(accessTokenGeneratorPort,
                refreshTokenRepositoryPort, 7L);
        servicio = new CompletarLoginMfaService(mfaChallengeTokenPort, usuarioRepositoryPort, totpPort,
                secretEncryptorPort, codigoRespaldoRepositoryPort, emisionTokensService);
    }

    private static Usuario usuarioConMfaHabilitado(UUID id) {
        return Usuario.reconstruir(id, Email.de("ana@ejemplo.com"), "Ana Pérez", HASH, EstadoUsuario.ACTIVO,
                0, null, true, "secreto-cifrado", null, Instant.now(), Instant.now());
    }

    @Test
    void completarConCodigoTotpValidoEmiteTokensYReseteaIntentos() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuarioConMfaHabilitado(usuarioId);
        when(mfaChallengeTokenPort.validar("desafio-valido")).thenReturn(usuarioId);
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(secretEncryptorPort.desencriptar("secreto-cifrado")).thenReturn("SECRETOPLANO");
        when(totpPort.verificarCodigo("SECRETOPLANO", "123456")).thenReturn(true);
        when(usuarioRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accessTokenGeneratorPort.generar(any())).thenReturn("access-token-de-prueba");
        when(refreshTokenRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginResultado.Completado resultado = servicio.completar(
                new CompletarLoginMfaComando("desafio-valido", "123456"));

        assertThat(resultado.accessToken()).isEqualTo("access-token-de-prueba");
        assertThat(resultado.refreshTokenValor()).isNotBlank();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getIntentosFallidos()).isZero();

        verify(codigoRespaldoRepositoryPort, never()).buscarNoUsadoPorHash(any(), any());
        verify(refreshTokenRepositoryPort).guardar(any(RefreshToken.class));
    }

    @Test
    void completarConCodigoDeRespaldoValidoLoMarcaComoUsado() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuarioConMfaHabilitado(usuarioId);
        CodigoRespaldo codigoNoUsado = CodigoRespaldo.crear(usuarioId, "hash-cualquiera", Instant.now());
        when(mfaChallengeTokenPort.validar("desafio-valido")).thenReturn(usuarioId);
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(secretEncryptorPort.desencriptar("secreto-cifrado")).thenReturn("SECRETOPLANO");
        when(totpPort.verificarCodigo(eq("SECRETOPLANO"), any())).thenReturn(false);
        when(codigoRespaldoRepositoryPort.buscarNoUsadoPorHash(eq(usuarioId), any()))
                .thenReturn(Optional.of(codigoNoUsado));
        when(usuarioRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accessTokenGeneratorPort.generar(any())).thenReturn("access-token-de-prueba");
        when(refreshTokenRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.completar(new CompletarLoginMfaComando("desafio-valido", "ABCD2345"));

        ArgumentCaptor<CodigoRespaldo> captor = ArgumentCaptor.forClass(CodigoRespaldo.class);
        verify(codigoRespaldoRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().estaUsado()).isTrue();
    }

    @Test
    void rechazaUnCodigoIncorrectoYRegistraIntentoFallido() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuarioConMfaHabilitado(usuarioId);
        when(mfaChallengeTokenPort.validar("desafio-valido")).thenReturn(usuarioId);
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(secretEncryptorPort.desencriptar("secreto-cifrado")).thenReturn("SECRETOPLANO");
        when(totpPort.verificarCodigo(eq("SECRETOPLANO"), any())).thenReturn(false);
        when(codigoRespaldoRepositoryPort.buscarNoUsadoPorHash(eq(usuarioId), any())).thenReturn(Optional.empty());
        when(usuarioRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> servicio.completar(new CompletarLoginMfaComando("desafio-valido", "000000")))
                .isInstanceOf(CodigoMfaInvalidoException.class);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getIntentosFallidos()).isEqualTo(1);
        verify(accessTokenGeneratorPort, never()).generar(any());
    }

    @Test
    void rechazaUnTokenDeDesafioInvalido() {
        when(mfaChallengeTokenPort.validar("desafio-invalido")).thenThrow(new TokenInvalidoException());

        assertThatThrownBy(() -> servicio.completar(new CompletarLoginMfaComando("desafio-invalido", "123456")))
                .isInstanceOf(TokenInvalidoException.class);

        verify(usuarioRepositoryPort, never()).buscarPorId(any());
    }

    @Test
    void rechazaSiElUsuarioYaNoExisteOQuedoInactivo() {
        UUID usuarioId = UUID.randomUUID();
        when(mfaChallengeTokenPort.validar("desafio-valido")).thenReturn(usuarioId);
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.completar(new CompletarLoginMfaComando("desafio-valido", "123456")))
                .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void rechazaSiLaCuentaQuedoBloqueadaTemporalmenteEntreElPrimerYSegundoPaso() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuarioBloqueado = Usuario.reconstruir(usuarioId, Email.de("ana@ejemplo.com"), "Ana Pérez", HASH,
                EstadoUsuario.ACTIVO, 3, Instant.now().plusSeconds(60), true, "secreto-cifrado", null,
                Instant.now(), Instant.now());
        when(mfaChallengeTokenPort.validar("desafio-valido")).thenReturn(usuarioId);
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuarioBloqueado));

        assertThatThrownBy(() -> servicio.completar(new CompletarLoginMfaComando("desafio-valido", "123456")))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(totpPort, never()).verificarCodigo(any(), any());
    }
}
