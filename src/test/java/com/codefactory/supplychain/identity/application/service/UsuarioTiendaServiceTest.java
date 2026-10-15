package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.AsignarTiendaComando;
import com.codefactory.supplychain.identity.application.port.out.TiendaAsignable;
import com.codefactory.supplychain.identity.application.port.out.TiendaConsultaPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioTiendaRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.TiendaNoEncontradaException;
import com.codefactory.supplychain.identity.domain.exception.UsuarioNoEncontradoException;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioTiendaServiceTest {

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID tiendaId = UUID.randomUUID();

    private UsuarioRepositoryPort usuarioRepositoryPort;
    private UsuarioTiendaRepositoryPort usuarioTiendaRepositoryPort;
    private TiendaConsultaPort tiendaConsultaPort;
    private UsuarioTiendaService servicio;

    @BeforeEach
    void setUp() {
        usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        usuarioTiendaRepositoryPort = mock(UsuarioTiendaRepositoryPort.class);
        tiendaConsultaPort = mock(TiendaConsultaPort.class);
        servicio = new UsuarioTiendaService(usuarioRepositoryPort, usuarioTiendaRepositoryPort, tiendaConsultaPort);
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(
                Usuario.crear(Email.de("vendedor@ejemplo.com"), "Vendedor", PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv"))));
    }

    @Test
    void asignaUnaTiendaExistenteQueNoTeniaAsignada() {
        when(tiendaConsultaPort.buscar(tiendaId)).thenReturn(Optional.of(new TiendaAsignable(tiendaId, "Centro", true)));

        servicio.asignar(new AsignarTiendaComando(usuarioId, tiendaId));

        verify(usuarioTiendaRepositoryPort).asignar(usuarioId, tiendaId);
    }

    @Test
    void permiteAsignarUnaTiendaInactiva() {
        when(tiendaConsultaPort.buscar(tiendaId)).thenReturn(Optional.of(new TiendaAsignable(tiendaId, "Cerrada", false)));

        servicio.asignar(new AsignarTiendaComando(usuarioId, tiendaId));

        verify(usuarioTiendaRepositoryPort).asignar(usuarioId, tiendaId);
    }

    @Test
    void asignarNoFallaNiDuplicaSiYaEstabaAsignada() {
        when(tiendaConsultaPort.buscar(tiendaId)).thenReturn(Optional.of(new TiendaAsignable(tiendaId, "Centro", true)));
        when(usuarioTiendaRepositoryPort.existeAsignacion(usuarioId, tiendaId)).thenReturn(true);

        servicio.asignar(new AsignarTiendaComando(usuarioId, tiendaId));

        verify(usuarioTiendaRepositoryPort, never()).asignar(any(), any());
    }

    @Test
    void rechazaAsignarUnaTiendaQueNoExiste() {
        when(tiendaConsultaPort.buscar(tiendaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(new AsignarTiendaComando(usuarioId, tiendaId)))
                .isInstanceOf(TiendaNoEncontradaException.class);
        verify(usuarioTiendaRepositoryPort, never()).asignar(any(), any());
    }

    @Test
    void rechazaAsignarAUnUsuarioQueNoExiste() {
        UUID inexistente = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(inexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(new AsignarTiendaComando(inexistente, tiendaId)))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void listaLasTiendasAsignadasConSusDatos() {
        TiendaAsignable tienda = new TiendaAsignable(tiendaId, "Centro", true);
        when(usuarioTiendaRepositoryPort.listarTiendasDeUsuario(usuarioId)).thenReturn(List.of(tiendaId));
        when(tiendaConsultaPort.buscarTodas(List.of(tiendaId))).thenReturn(List.of(tienda));

        assertThat(servicio.listarTiendas(usuarioId)).containsExactly(tienda);
    }

    @Test
    void quitarDelegaSinValidarNada() {
        servicio.quitar(new AsignarTiendaComando(usuarioId, tiendaId));

        verify(usuarioTiendaRepositoryPort).quitar(usuarioId, tiendaId);
    }
}
