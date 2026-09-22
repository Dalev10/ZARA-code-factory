package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.ConfirmarMfaComando;
import com.codefactory.supplychain.identity.application.port.in.ConfirmarMfaResultado;
import com.codefactory.supplychain.identity.application.port.out.CodigoRespaldoRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.SecretEncryptorPort;
import com.codefactory.supplychain.identity.application.port.out.TotpPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.CodigoMfaInvalidoException;
import com.codefactory.supplychain.identity.domain.exception.MfaNoConfiguradoException;
import com.codefactory.supplychain.identity.domain.model.CodigoRespaldo;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
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

class ConfirmarMfaServiceTest {

    private static final PasswordHash HASH = PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv");

    private UsuarioRepositoryPort usuarioRepositoryPort;
    private TotpPort totpPort;
    private SecretEncryptorPort secretEncryptorPort;
    private CodigoRespaldoRepositoryPort codigoRespaldoRepositoryPort;
    private ConfirmarMfaService servicio;

    @BeforeEach
    void setUp() {
        usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        totpPort = mock(TotpPort.class);
        secretEncryptorPort = mock(SecretEncryptorPort.class);
        codigoRespaldoRepositoryPort = mock(CodigoRespaldoRepositoryPort.class);
        servicio = new ConfirmarMfaService(usuarioRepositoryPort, totpPort, secretEncryptorPort,
                codigoRespaldoRepositoryPort);
    }

    private static Usuario usuarioConMfaPendiente(UUID id) {
        return Usuario.reconstruir(id, Email.de("ana@ejemplo.com"), "Ana Pérez", HASH, EstadoUsuario.ACTIVO,
                0, null, false, "secreto-cifrado", null, Instant.now(), Instant.now());
    }

    @Test
    void confirmarConCodigoValidoHabilitaMfaYGeneraOchoCodigosDeRespaldo() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuarioConMfaPendiente(usuarioId);
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(secretEncryptorPort.desencriptar("secreto-cifrado")).thenReturn("SECRETOPLANO");
        when(totpPort.verificarCodigo("SECRETOPLANO", "123456")).thenReturn(true);
        when(usuarioRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(codigoRespaldoRepositoryPort.guardarTodos(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfirmarMfaResultado resultado = servicio.confirmar(new ConfirmarMfaComando(usuarioId, "123456"));

        assertThat(resultado.codigosRespaldo()).hasSize(8);
        assertThat(resultado.codigosRespaldo()).doesNotHaveDuplicates();

        ArgumentCaptor<Usuario> captorUsuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepositoryPort).guardar(captorUsuario.capture());
        assertThat(captorUsuario.getValue().isMfaHabilitado()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CodigoRespaldo>> captorCodigos = ArgumentCaptor.forClass(List.class);
        verify(codigoRespaldoRepositoryPort).guardarTodos(captorCodigos.capture());
        assertThat(captorCodigos.getValue()).hasSize(8);
        assertThat(captorCodigos.getValue()).allMatch(c -> c.getUsuarioId().equals(usuarioId));
    }

    @Test
    void rechazaUnCodigoIncorrectoSinHabilitarMfa() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuarioConMfaPendiente(usuarioId);
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(secretEncryptorPort.desencriptar("secreto-cifrado")).thenReturn("SECRETOPLANO");
        when(totpPort.verificarCodigo(eq("SECRETOPLANO"), any())).thenReturn(false);

        assertThatThrownBy(() -> servicio.confirmar(new ConfirmarMfaComando(usuarioId, "000000")))
                .isInstanceOf(CodigoMfaInvalidoException.class);

        verify(usuarioRepositoryPort, never()).guardar(any());
        verify(codigoRespaldoRepositoryPort, never()).guardarTodos(any());
    }

    @Test
    void rechazaSiNuncaSeInicioLaActivacion() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuarioSinMfa = Usuario.reconstruir(usuarioId, Email.de("ana@ejemplo.com"), "Ana Pérez", HASH,
                EstadoUsuario.ACTIVO, 0, null, false, null, null, Instant.now(), Instant.now());
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuarioSinMfa));

        assertThatThrownBy(() -> servicio.confirmar(new ConfirmarMfaComando(usuarioId, "123456")))
                .isInstanceOf(MfaNoConfiguradoException.class);

        verify(secretEncryptorPort, never()).desencriptar(any());
    }
}
