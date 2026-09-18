package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.ActivarMfaComando;
import com.codefactory.supplychain.identity.application.port.in.ActivarMfaResultado;
import com.codefactory.supplychain.identity.application.port.out.SecretEncryptorPort;
import com.codefactory.supplychain.identity.application.port.out.TotpPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.MfaYaActivoException;
import com.codefactory.supplychain.identity.domain.exception.TokenInvalidoException;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivarMfaServiceTest {

    private static final PasswordHash HASH = PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv");

    private UsuarioRepositoryPort usuarioRepositoryPort;
    private TotpPort totpPort;
    private SecretEncryptorPort secretEncryptorPort;
    private ActivarMfaService servicio;

    @BeforeEach
    void setUp() {
        usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        totpPort = mock(TotpPort.class);
        secretEncryptorPort = mock(SecretEncryptorPort.class);
        servicio = new ActivarMfaService(usuarioRepositoryPort, totpPort, secretEncryptorPort);
    }

    private static Usuario usuarioActivo(UUID id) {
        return Usuario.reconstruir(id, Email.de("ana@ejemplo.com"), "Ana Pérez", HASH, EstadoUsuario.ACTIVO,
                0, null, false, null, null, Instant.now(), Instant.now());
    }

    @Test
    void activarGeneraElSecretoLoCifraYDevuelveElQr() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuarioActivo(usuarioId);
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario));
        when(totpPort.generarSecreto()).thenReturn("SECRETOPLANO");
        when(secretEncryptorPort.encriptar("SECRETOPLANO")).thenReturn("secreto-cifrado");
        when(totpPort.generarQrDataUri("SECRETOPLANO", "ana@ejemplo.com")).thenReturn("data:image/png;base64,xyz");
        when(usuarioRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        ActivarMfaResultado resultado = servicio.activar(new ActivarMfaComando(usuarioId));

        assertThat(resultado.secretoBase32()).isEqualTo("SECRETOPLANO");
        assertThat(resultado.qrCodeDataUri()).isEqualTo("data:image/png;base64,xyz");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepositoryPort).guardar(captor.capture());
        assertThat(captor.getValue().getMfaSecretEncrypted()).isEqualTo("secreto-cifrado");
        assertThat(captor.getValue().isMfaHabilitado()).isFalse();
    }

    @Test
    void rechazaSiElUsuarioNoExiste() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.activar(new ActivarMfaComando(usuarioId)))
                .isInstanceOf(TokenInvalidoException.class);
    }

    @Test
    void rechazaSiMfaYaEstaHabilitado() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuarioConMfa = Usuario.reconstruir(usuarioId, Email.de("ana@ejemplo.com"), "Ana Pérez", HASH,
                EstadoUsuario.ACTIVO, 0, null, true, "secreto-existente", null, Instant.now(), Instant.now());
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuarioConMfa));
        when(totpPort.generarSecreto()).thenReturn("SECRETOPLANO");
        when(secretEncryptorPort.encriptar(eq("SECRETOPLANO"))).thenReturn("secreto-cifrado");

        assertThatThrownBy(() -> servicio.activar(new ActivarMfaComando(usuarioId)))
                .isInstanceOf(MfaYaActivoException.class);
    }
}
