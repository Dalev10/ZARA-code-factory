package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.LoginComando;
import com.codefactory.supplychain.identity.application.port.out.AccessTokenGeneratorPort;
import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.RefreshTokenRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.CredencialesInvalidasException;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.RefreshToken;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private static final PasswordHash HASH = PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv");

    private UsuarioRepositoryPort usuarioRepositoryPort;
    private PasswordHasherPort passwordHasherPort;
    private AccessTokenGeneratorPort accessTokenGeneratorPort;
    private RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private LoginService servicio;

    @BeforeEach
    void setUp() {
        usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        passwordHasherPort = mock(PasswordHasherPort.class);
        accessTokenGeneratorPort = mock(AccessTokenGeneratorPort.class);
        refreshTokenRepositoryPort = mock(RefreshTokenRepositoryPort.class);
        servicio = new LoginService(usuarioRepositoryPort, passwordHasherPort, accessTokenGeneratorPort,
                refreshTokenRepositoryPort, 7L);
    }

    private static Usuario usuarioActivo(String email) {
        return Usuario.reconstruir(UUID.randomUUID(), Email.de(email), "Usuario de Prueba", HASH,
                EstadoUsuario.ACTIVO, 0, null, false, null, null, Instant.now(), Instant.now());
    }

    @Test
    void loginExitosoEmiteAccessYRefreshToken() {
        Usuario usuario = usuarioActivo("ana@ejemplo.com");
        when(usuarioRepositoryPort.buscarPorEmail(Email.de("ana@ejemplo.com"))).thenReturn(Optional.of(usuario));
        when(passwordHasherPort.coincide(eq("contraseñaCorrecta123"), eq(HASH))).thenReturn(true);
        when(accessTokenGeneratorPort.generar(usuario)).thenReturn("access-token-de-prueba");
        when(refreshTokenRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        var resultado = servicio.login(new LoginComando("ana@ejemplo.com", "contraseñaCorrecta123"));

        assertThat(resultado.accessToken()).isEqualTo("access-token-de-prueba");
        assertThat(resultado.refreshTokenValor()).isNotBlank();
        assertThat(resultado.usuario()).isEqualTo(usuario);
        assertThat(resultado.refreshTokenExpiraEn()).isAfter(Instant.now());

        verify(refreshTokenRepositoryPort).guardar(any(RefreshToken.class));
    }

    @Test
    void rechazaPasswordIncorrecta() {
        Usuario usuario = usuarioActivo("ana@ejemplo.com");
        when(usuarioRepositoryPort.buscarPorEmail(Email.de("ana@ejemplo.com"))).thenReturn(Optional.of(usuario));
        when(passwordHasherPort.coincide(anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> servicio.login(new LoginComando("ana@ejemplo.com", "incorrecta")))
                .isInstanceOf(CredencialesInvalidasException.class);

        verify(accessTokenGeneratorPort, never()).generar(any());
        verify(refreshTokenRepositoryPort, never()).guardar(any());
    }

    @Test
    void rechazaEmailInexistentePeroIgualConsultaElHasherParaEvitarTimingAttack() {
        when(usuarioRepositoryPort.buscarPorEmail(any())).thenReturn(Optional.empty());
        when(passwordHasherPort.coincide(anyString(), any())).thenReturn(false);

        assertThatThrownBy(() -> servicio.login(new LoginComando("noexiste@ejemplo.com", "cualquiera")))
                .isInstanceOf(CredencialesInvalidasException.class);

        // Aunque el usuario no exista, se llama al hasher igual (contra un hash señuelo)
        // para que el tiempo de respuesta no delate si la cuenta existe o no.
        verify(passwordHasherPort).coincide(eq("cualquiera"), any());
        verify(accessTokenGeneratorPort, never()).generar(any());
    }

    @Test
    void rechazaCuentaBloqueadaConElMismoMensajeGenerico() {
        Usuario usuarioBloqueado = Usuario.reconstruir(UUID.randomUUID(), Email.de("bloqueado@ejemplo.com"),
                "Usuario Bloqueado", HASH, EstadoUsuario.BLOQUEADO, 5, Instant.now().plusSeconds(3600), false,
                null, null, Instant.now(), Instant.now());
        when(usuarioRepositoryPort.buscarPorEmail(Email.de("bloqueado@ejemplo.com")))
                .thenReturn(Optional.of(usuarioBloqueado));
        when(passwordHasherPort.coincide(anyString(), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.login(new LoginComando("bloqueado@ejemplo.com", "contraseñaCorrecta123")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Credenciales inválidas");

        verify(accessTokenGeneratorPort, never()).generar(any());
    }

    @Test
    void rechazaCuentaInactiva() {
        Usuario usuarioInactivo = Usuario.reconstruir(UUID.randomUUID(), Email.de("inactivo@ejemplo.com"),
                "Usuario Inactivo", HASH, EstadoUsuario.INACTIVO, 0, null, false, null, null,
                Instant.now(), Instant.now());
        when(usuarioRepositoryPort.buscarPorEmail(Email.de("inactivo@ejemplo.com")))
                .thenReturn(Optional.of(usuarioInactivo));
        when(passwordHasherPort.coincide(anyString(), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.login(new LoginComando("inactivo@ejemplo.com", "contraseñaCorrecta123")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
