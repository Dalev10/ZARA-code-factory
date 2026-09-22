package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.RegistrarUsuarioComando;
import com.codefactory.supplychain.identity.application.port.out.PasswordComprometidaPort;
import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.EmailYaRegistradoException;
import com.codefactory.supplychain.identity.domain.exception.PasswordComprometidaException;
import com.codefactory.supplychain.identity.domain.exception.PasswordDebilException;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrarUsuarioServiceTest {

    private static final PasswordHash HASH = PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv");

    private UsuarioRepositoryPort usuarioRepositoryPort;
    private PasswordHasherPort passwordHasherPort;
    private PasswordComprometidaPort passwordComprometidaPort;
    private RegistrarUsuarioService servicio;

    @BeforeEach
    void setUp() {
        usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        passwordHasherPort = mock(PasswordHasherPort.class);
        passwordComprometidaPort = mock(PasswordComprometidaPort.class);
        servicio = new RegistrarUsuarioService(usuarioRepositoryPort, passwordHasherPort, passwordComprometidaPort);
    }

    @Test
    void registraUnUsuarioNuevoCorrectamente() {
        when(usuarioRepositoryPort.existePorEmail(any())).thenReturn(false);
        when(passwordComprometidaPort.estaComprometida(any())).thenReturn(false);
        when(passwordHasherPort.hashear(any())).thenReturn(HASH);
        when(usuarioRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario usuario = servicio.registrar(
                new RegistrarUsuarioComando("nueva@ejemplo.com", "Ana Pérez", "contraseñaSegura123"));

        assertThat(usuario.getEmail()).isEqualTo(Email.de("nueva@ejemplo.com"));
        assertThat(usuario.getPasswordHash()).isEqualTo(HASH);

        ArgumentCaptor<Password> passwordCaptor = ArgumentCaptor.forClass(Password.class);
        verify(passwordHasherPort).hashear(passwordCaptor.capture());
        assertThat(passwordCaptor.getValue().getValor()).isEqualTo("contraseñaSegura123");
    }

    @Test
    void rechazaEmailYaRegistradoSinConsultarHibpNiHashear() {
        when(usuarioRepositoryPort.existePorEmail(any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(
                new RegistrarUsuarioComando("existe@ejemplo.com", "Ana Pérez", "contraseñaSegura123")))
                .isInstanceOf(EmailYaRegistradoException.class);

        verify(passwordComprometidaPort, never()).estaComprometida(any());
        verify(passwordHasherPort, never()).hashear(any());
        verify(usuarioRepositoryPort, never()).guardar(any());
    }

    @Test
    void rechazaPasswordDebilAntesDeConsultarHibp() {
        when(usuarioRepositoryPort.existePorEmail(any())).thenReturn(false);

        assertThatThrownBy(() -> servicio.registrar(
                new RegistrarUsuarioComando("nueva@ejemplo.com", "Ana Pérez", "corta")))
                .isInstanceOf(PasswordDebilException.class);

        verify(passwordComprometidaPort, never()).estaComprometida(any());
        verify(usuarioRepositoryPort, never()).guardar(any());
    }

    @Test
    void rechazaPasswordComprometidaSinGuardarElUsuario() {
        when(usuarioRepositoryPort.existePorEmail(any())).thenReturn(false);
        when(passwordComprometidaPort.estaComprometida(any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(
                new RegistrarUsuarioComando("nueva@ejemplo.com", "Ana Pérez", "contraseñaFiltrada123")))
                .isInstanceOf(PasswordComprometidaException.class);

        verify(passwordHasherPort, never()).hashear(any());
        verify(usuarioRepositoryPort, never()).guardar(any());
    }
}
