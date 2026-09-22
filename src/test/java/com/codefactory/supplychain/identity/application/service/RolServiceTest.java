package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.ActualizarRolComando;
import com.codefactory.supplychain.identity.application.port.in.AsignarScopeComando;
import com.codefactory.supplychain.identity.application.port.in.CrearRolComando;
import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.RolScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.ScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRolRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.RolEnUsoException;
import com.codefactory.supplychain.identity.domain.exception.RolNoEncontradoException;
import com.codefactory.supplychain.identity.domain.exception.RolYaExistenteException;
import com.codefactory.supplychain.identity.domain.exception.ScopeNoEncontradoException;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Scope;
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

class RolServiceTest {

    private RolRepositoryPort rolRepositoryPort;
    private ScopeRepositoryPort scopeRepositoryPort;
    private RolScopeRepositoryPort rolScopeRepositoryPort;
    private UsuarioRolRepositoryPort usuarioRolRepositoryPort;
    private RolService servicio;

    @BeforeEach
    void setUp() {
        rolRepositoryPort = mock(RolRepositoryPort.class);
        scopeRepositoryPort = mock(ScopeRepositoryPort.class);
        rolScopeRepositoryPort = mock(RolScopeRepositoryPort.class);
        usuarioRolRepositoryPort = mock(UsuarioRolRepositoryPort.class);
        servicio = new RolService(rolRepositoryPort, scopeRepositoryPort, rolScopeRepositoryPort,
                usuarioRolRepositoryPort);
    }

    @Test
    void creaUnRolNuevo() {
        when(rolRepositoryPort.existePorNombre("VENDEDOR")).thenReturn(false);
        when(rolRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Rol rol = servicio.crear(new CrearRolComando("VENDEDOR", "desc"));

        assertThat(rol.getNombre()).isEqualTo("VENDEDOR");
        verify(rolRepositoryPort).guardar(any(Rol.class));
    }

    @Test
    void rechazaCrearUnRolConNombreDuplicado() {
        when(rolRepositoryPort.existePorNombre("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(new CrearRolComando("ADMIN", "desc")))
                .isInstanceOf(RolYaExistenteException.class);

        verify(rolRepositoryPort, never()).guardar(any());
    }

    @Test
    void actualizaUnRolExistente() {
        UUID rolId = UUID.randomUUID();
        Rol existente = Rol.reconstruir(rolId, "VENDEDOR", "desc vieja", java.time.Instant.now());
        when(rolRepositoryPort.buscarPorId(rolId)).thenReturn(Optional.of(existente));
        when(rolRepositoryPort.existePorNombre("VENDEDOR_SENIOR")).thenReturn(false);
        when(rolRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Rol actualizado = servicio.actualizar(new ActualizarRolComando(rolId, "VENDEDOR_SENIOR", "desc nueva"));

        assertThat(actualizado.getNombre()).isEqualTo("VENDEDOR_SENIOR");
        assertThat(actualizado.getDescripcion()).isEqualTo("desc nueva");
    }

    @Test
    void actualizarPermiteConservarElMismoNombre() {
        UUID rolId = UUID.randomUUID();
        Rol existente = Rol.reconstruir(rolId, "VENDEDOR", "desc vieja", java.time.Instant.now());
        when(rolRepositoryPort.buscarPorId(rolId)).thenReturn(Optional.of(existente));
        when(rolRepositoryPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.actualizar(new ActualizarRolComando(rolId, "VENDEDOR", "desc nueva"));

        verify(rolRepositoryPort, never()).existePorNombre(any());
    }

    @Test
    void actualizarRechazaRenombrarAUnNombreYaUsadoPorOtroRol() {
        UUID rolId = UUID.randomUUID();
        Rol existente = Rol.reconstruir(rolId, "VENDEDOR", "desc", java.time.Instant.now());
        when(rolRepositoryPort.buscarPorId(rolId)).thenReturn(Optional.of(existente));
        when(rolRepositoryPort.existePorNombre("ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(new ActualizarRolComando(rolId, "ADMIN", "desc")))
                .isInstanceOf(RolYaExistenteException.class);
    }

    @Test
    void actualizarRechazaUnRolInexistente() {
        UUID rolId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(new ActualizarRolComando(rolId, "X", "Y")))
                .isInstanceOf(RolNoEncontradoException.class);
    }

    @Test
    void eliminaUnRolSinUsuariosAsignados() {
        UUID rolId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolId))
                .thenReturn(Optional.of(Rol.reconstruir(rolId, "X", null, java.time.Instant.now())));
        when(usuarioRolRepositoryPort.tieneUsuariosAsignados(rolId)).thenReturn(false);

        servicio.eliminar(rolId);

        verify(rolRepositoryPort).eliminar(rolId);
    }

    @Test
    void rechazaEliminarUnRolConUsuariosAsignados() {
        UUID rolId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolId))
                .thenReturn(Optional.of(Rol.reconstruir(rolId, "ADMIN", null, java.time.Instant.now())));
        when(usuarioRolRepositoryPort.tieneUsuariosAsignados(rolId)).thenReturn(true);

        assertThatThrownBy(() -> servicio.eliminar(rolId)).isInstanceOf(RolEnUsoException.class);

        verify(rolRepositoryPort, never()).eliminar(any());
    }

    @Test
    void rechazaEliminarUnRolInexistente() {
        UUID rolId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(rolId)).isInstanceOf(RolNoEncontradoException.class);
    }

    @Test
    void asignaUnScopeAUnRolSiNoEstabaAsignadoAntes() {
        UUID rolId = UUID.randomUUID();
        UUID scopeId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolId))
                .thenReturn(Optional.of(Rol.reconstruir(rolId, "X", null, java.time.Instant.now())));
        when(scopeRepositoryPort.buscarPorId(scopeId))
                .thenReturn(Optional.of(Scope.reconstruir(scopeId, "c", null, false, java.time.Instant.now())));
        when(rolScopeRepositoryPort.existeAsignacion(rolId, scopeId)).thenReturn(false);

        servicio.asignar(new AsignarScopeComando(rolId, scopeId));

        verify(rolScopeRepositoryPort).asignar(rolId, scopeId);
    }

    @Test
    void asignarEsIdempotenteSiYaEstabaAsignado() {
        UUID rolId = UUID.randomUUID();
        UUID scopeId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolId))
                .thenReturn(Optional.of(Rol.reconstruir(rolId, "X", null, java.time.Instant.now())));
        when(scopeRepositoryPort.buscarPorId(scopeId))
                .thenReturn(Optional.of(Scope.reconstruir(scopeId, "c", null, false, java.time.Instant.now())));
        when(rolScopeRepositoryPort.existeAsignacion(rolId, scopeId)).thenReturn(true);

        servicio.asignar(new AsignarScopeComando(rolId, scopeId));

        verify(rolScopeRepositoryPort, never()).asignar(any(), any());
    }

    @Test
    void rechazaAsignarUnScopeInexistente() {
        UUID rolId = UUID.randomUUID();
        UUID scopeId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolId))
                .thenReturn(Optional.of(Rol.reconstruir(rolId, "X", null, java.time.Instant.now())));
        when(scopeRepositoryPort.buscarPorId(scopeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(new AsignarScopeComando(rolId, scopeId)))
                .isInstanceOf(ScopeNoEncontradoException.class);
    }

    @Test
    void quitarUnScopeEsSiempreIdempotente() {
        UUID rolId = UUID.randomUUID();
        UUID scopeId = UUID.randomUUID();

        servicio.quitar(new AsignarScopeComando(rolId, scopeId));

        verify(rolScopeRepositoryPort).quitar(rolId, scopeId);
    }

    @Test
    void listaLosScopesDeUnRolExistente() {
        UUID rolId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolId))
                .thenReturn(Optional.of(Rol.reconstruir(rolId, "X", null, java.time.Instant.now())));
        Scope scope = Scope.reconstruir(UUID.randomUUID(), "c", null, false, java.time.Instant.now());
        when(rolScopeRepositoryPort.listarScopesDeRol(rolId)).thenReturn(List.of(scope));

        List<Scope> scopes = servicio.listar(rolId);

        assertThat(scopes).containsExactly(scope);
    }

    @Test
    void rechazaListarScopesDeUnRolInexistente() {
        UUID rolId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.listar(rolId)).isInstanceOf(RolNoEncontradoException.class);
    }
}
