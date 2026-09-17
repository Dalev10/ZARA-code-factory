package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.RefrescarTokenComando;
import com.codefactory.supplychain.identity.application.port.out.AccessTokenGeneratorPort;
import com.codefactory.supplychain.identity.application.port.out.RefreshTokenRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.TokenInvalidoException;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefrescarTokenServiceTest {

    private static final PasswordHash HASH = PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv");

    private RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private UsuarioRepositoryPort usuarioRepositoryPort;
    private AccessTokenGeneratorPort accessTokenGeneratorPort;
    private RefrescarTokenService servicio;

    @BeforeEach
    void setUp() {
        refreshTokenRepositoryPort = mock(RefreshTokenRepositoryPort.class);
        usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        accessTokenGeneratorPort = mock(AccessTokenGeneratorPort.class);
        servicio = new RefrescarTokenService(refreshTokenRepositoryPort, usuarioRepositoryPort,
                accessTokenGeneratorPort, 7L);
    }

    private static Usuario usuarioActivo(UUID id) {
        return Usuario.reconstruir(id, Email.de("ana@ejemplo.com"), "Ana Pérez", HASH, EstadoUsuario.ACTIVO,
                0, null, false, null, null, Instant.now(), Instant.now());
    }

    @Test
    void refrescoExitosoRotaElTokenYEmiteUnAccessTokenNuevo() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuarioActivo(usuarioId);
        Instant ahora = Instant.now();
        RefreshToken tokenVigente = RefreshToken.crearNuevaFamilia(usuarioId, "hash-vigente", ahora,
                ahora.plusSeconds(3600));

        when(refreshTokenRepositoryPort.buscarPorTokenHash(any())).thenReturn(Optional.of(tokenVigente));
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(refreshTokenRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accessTokenGeneratorPort.generar(usuario)).thenReturn("nuevo-access-token");

        var resultado = servicio.refrescar(new RefrescarTokenComando("valor-cualquiera"));

        assertThat(resultado.accessToken()).isEqualTo("nuevo-access-token");
        assertThat(resultado.nuevoRefreshTokenValor()).isNotBlank();
        assertThat(resultado.nuevoRefreshTokenExpiraEn()).isAfter(ahora);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepositoryPort, times(2)).guardar(captor.capture());
        assertThat(captor.getAllValues().get(0).estaRevocado()).isTrue();
        assertThat(captor.getAllValues().get(0).getId()).isEqualTo(tokenVigente.getId());
        assertThat(captor.getAllValues().get(1).getFamiliaId()).isEqualTo(tokenVigente.getFamiliaId());
        assertThat(captor.getAllValues().get(1).estaRevocado()).isFalse();

        verify(refreshTokenRepositoryPort, never()).revocarFamilia(any(), any());
    }

    @Test
    void tokenDesconocidoSeRechaza() {
        when(refreshTokenRepositoryPort.buscarPorTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.refrescar(new RefrescarTokenComando("no-existe")))
                .isInstanceOf(TokenInvalidoException.class);

        verify(accessTokenGeneratorPort, never()).generar(any());
    }

    @Test
    void unTokenYaRevocadoDisparaLaRevocacionDeTodaLaFamilia() {
        UUID usuarioId = UUID.randomUUID();
        Instant ahora = Instant.now();
        UUID familiaId = UUID.randomUUID();
        RefreshToken tokenYaUsado = RefreshToken.reconstruir(UUID.randomUUID(), usuarioId, familiaId, "hash-viejo",
                ahora.minusSeconds(60), ahora.plusSeconds(3600), ahora.minusSeconds(30));

        when(refreshTokenRepositoryPort.buscarPorTokenHash(any())).thenReturn(Optional.of(tokenYaUsado));

        assertThatThrownBy(() -> servicio.refrescar(new RefrescarTokenComando("token-robado")))
                .isInstanceOf(TokenInvalidoException.class);

        verify(refreshTokenRepositoryPort).revocarFamilia(eq(familiaId), any());
        verify(accessTokenGeneratorPort, never()).generar(any());
        verify(usuarioRepositoryPort, never()).buscarPorId(any());
    }

    @Test
    void unTokenExpiradoSeRechazaSinDispararRevocacionDeFamilia() {
        UUID usuarioId = UUID.randomUUID();
        Instant ahora = Instant.now();
        RefreshToken tokenExpirado = RefreshToken.crearNuevaFamilia(usuarioId, "hash-expirado",
                ahora.minusSeconds(7200), ahora.minusSeconds(3600));

        when(refreshTokenRepositoryPort.buscarPorTokenHash(any())).thenReturn(Optional.of(tokenExpirado));

        assertThatThrownBy(() -> servicio.refrescar(new RefrescarTokenComando("token-expirado")))
                .isInstanceOf(TokenInvalidoException.class);

        verify(refreshTokenRepositoryPort, never()).revocarFamilia(any(), any());
        verify(accessTokenGeneratorPort, never()).generar(any());
    }

    @Test
    void seRechazaSiElUsuarioYaNoEstaActivo() {
        UUID usuarioId = UUID.randomUUID();
        Instant ahora = Instant.now();
        RefreshToken tokenVigente = RefreshToken.crearNuevaFamilia(usuarioId, "hash-vigente", ahora,
                ahora.plusSeconds(3600));
        Usuario usuarioBloqueado = Usuario.reconstruir(usuarioId, Email.de("ana@ejemplo.com"), "Ana Pérez", HASH,
                EstadoUsuario.BLOQUEADO, 0, null, false, null, null, ahora, ahora);

        when(refreshTokenRepositoryPort.buscarPorTokenHash(any())).thenReturn(Optional.of(tokenVigente));
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuarioBloqueado));

        assertThatThrownBy(() -> servicio.refrescar(new RefrescarTokenComando("valor-cualquiera")))
                .isInstanceOf(TokenInvalidoException.class);

        verify(accessTokenGeneratorPort, never()).generar(any());
    }
}
