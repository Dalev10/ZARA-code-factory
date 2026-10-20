package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.AsignarRolComando;
import com.codefactory.supplychain.identity.application.port.in.DesactivarUsuarioComando;
import com.codefactory.supplychain.identity.application.port.out.RefreshTokenRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRolRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.AutodesactivacionException;
import com.codefactory.supplychain.identity.domain.exception.RolNoEncontradoException;
import com.codefactory.supplychain.identity.domain.exception.UltimoAdminException;
import com.codefactory.supplychain.identity.domain.exception.UsuarioNoEncontradoException;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private static final PasswordHash HASH = PasswordHash.de("$2a$10$abcdefghijklmnopqrstuv");

    private UsuarioRepositoryPort usuarioRepositoryPort;
    private RolRepositoryPort rolRepositoryPort;
    private UsuarioRolRepositoryPort usuarioRolRepositoryPort;
    private RefreshTokenRepositoryPort refreshTokenRepositoryPort;
    private UsuarioService servicio;

    @BeforeEach
    void setUp() {
        usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        rolRepositoryPort = mock(RolRepositoryPort.class);
        usuarioRolRepositoryPort = mock(UsuarioRolRepositoryPort.class);
        refreshTokenRepositoryPort = mock(RefreshTokenRepositoryPort.class);
        servicio = new UsuarioService(usuarioRepositoryPort, rolRepositoryPort, usuarioRolRepositoryPort,
                refreshTokenRepositoryPort);
        when(usuarioRepositoryPort.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static Usuario usuario(UUID id) {
        return usuario(id, EstadoUsuario.ACTIVO);
    }

    private static Usuario usuario(UUID id, EstadoUsuario estado) {
        return Usuario.reconstruir(id, Email.de("ana@ejemplo.com"), "Ana Pérez", HASH, estado,
                0, null, false, null, null, Instant.now(), Instant.now());
    }

    private static Rol rolAdmin(UUID id) {
        return Rol.reconstruir(id, "ADMIN", null, Instant.now());
    }

    @Test
    void obtenerRechazaUnUsuarioInexistente() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(usuarioId)).isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void asignaUnRolSiNoEstabaAsignadoAntes() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(rolRepositoryPort.buscarPorId(rolId))
                .thenReturn(Optional.of(Rol.reconstruir(rolId, "OPERADOR", null, Instant.now())));
        when(usuarioRolRepositoryPort.existeAsignacion(usuarioId, rolId)).thenReturn(false);

        servicio.asignar(new AsignarRolComando(usuarioId, rolId));

        verify(usuarioRolRepositoryPort).asignar(usuarioId, rolId);
    }

    @Test
    void asignarEsIdempotenteSiYaEstabaAsignado() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(rolRepositoryPort.buscarPorId(rolId))
                .thenReturn(Optional.of(Rol.reconstruir(rolId, "OPERADOR", null, Instant.now())));
        when(usuarioRolRepositoryPort.existeAsignacion(usuarioId, rolId)).thenReturn(true);

        servicio.asignar(new AsignarRolComando(usuarioId, rolId));

        verify(usuarioRolRepositoryPort, never()).asignar(usuarioId, rolId);
    }

    @Test
    void rechazaAsignarUnUsuarioInexistente() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(new AsignarRolComando(usuarioId, rolId)))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void rechazaAsignarUnRolInexistente() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(rolRepositoryPort.buscarPorId(rolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.asignar(new AsignarRolComando(usuarioId, rolId)))
                .isInstanceOf(RolNoEncontradoException.class);
    }

    @Test
    void quitaUnRolNoAdminSinRestricciones() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolId))
                .thenReturn(Optional.of(Rol.reconstruir(rolId, "OPERADOR", null, Instant.now())));
        when(usuarioRolRepositoryPort.existeAsignacion(usuarioId, rolId)).thenReturn(true);

        servicio.quitar(new AsignarRolComando(usuarioId, rolId));

        verify(usuarioRolRepositoryPort).quitar(usuarioId, rolId);
    }

    @Test
    void quitarUnRolEsSiempreIdempotenteInclusoSiNoEstabaAsignado() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolId))
                .thenReturn(Optional.of(Rol.reconstruir(rolId, "OPERADOR", null, Instant.now())));
        when(usuarioRolRepositoryPort.existeAsignacion(usuarioId, rolId)).thenReturn(false);

        servicio.quitar(new AsignarRolComando(usuarioId, rolId));

        verify(usuarioRolRepositoryPort).quitar(usuarioId, rolId);
    }

    @Test
    void rechazaQuitarElRolAdminAlUnicoAdministradorActivo() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolAdminId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolAdminId)).thenReturn(Optional.of(rolAdmin(rolAdminId)));
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(usuarioRolRepositoryPort.existeAsignacion(usuarioId, rolAdminId)).thenReturn(true);
        // Puede haber otros ADMIN desactivados: no cuentan (HU-31).
        when(usuarioRolRepositoryPort.contarUsuariosActivosConRol(rolAdminId)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.quitar(new AsignarRolComando(usuarioId, rolAdminId)))
                .isInstanceOf(UltimoAdminException.class);

        verify(usuarioRolRepositoryPort, never()).quitar(usuarioId, rolAdminId);
    }

    @Test
    void permiteQuitarElRolAdminSiHayOtrosAdministradoresActivos() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolAdminId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolAdminId)).thenReturn(Optional.of(rolAdmin(rolAdminId)));
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(usuarioRolRepositoryPort.existeAsignacion(usuarioId, rolAdminId)).thenReturn(true);
        when(usuarioRolRepositoryPort.contarUsuariosActivosConRol(rolAdminId)).thenReturn(2L);

        servicio.quitar(new AsignarRolComando(usuarioId, rolAdminId));

        verify(usuarioRolRepositoryPort).quitar(usuarioId, rolAdminId);
    }

    @Test
    void permiteQuitarElRolAdminAUnAdministradorInactivoAunqueQuedeUnSoloActivo() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolAdminId = UUID.randomUUID();
        when(rolRepositoryPort.buscarPorId(rolAdminId)).thenReturn(Optional.of(rolAdmin(rolAdminId)));
        when(usuarioRepositoryPort.buscarPorId(usuarioId))
                .thenReturn(Optional.of(usuario(usuarioId, EstadoUsuario.INACTIVO)));
        when(usuarioRolRepositoryPort.existeAsignacion(usuarioId, rolAdminId)).thenReturn(true);
        when(usuarioRolRepositoryPort.contarUsuariosActivosConRol(rolAdminId)).thenReturn(1L);

        servicio.quitar(new AsignarRolComando(usuarioId, rolAdminId));

        verify(usuarioRolRepositoryPort).quitar(usuarioId, rolAdminId);
    }

    @Test
    void desactivaAlUsuarioYRevocaTodosSusRefreshTokens() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolAdminId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(rolRepositoryPort.buscarPorNombre("ADMIN")).thenReturn(Optional.of(rolAdmin(rolAdminId)));
        when(usuarioRolRepositoryPort.existeAsignacion(usuarioId, rolAdminId)).thenReturn(false);

        Usuario desactivado = servicio.desactivar(new DesactivarUsuarioComando(usuarioId, UUID.randomUUID()));

        assertThat(desactivado.getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
        verify(usuarioRepositoryPort).guardar(argThat(u -> u.getEstado() == EstadoUsuario.INACTIVO));
        verify(refreshTokenRepositoryPort).revocarTodosDeUsuario(eq(usuarioId), any());
    }

    @Test
    void rechazaQueUnUsuarioSeDesactiveASiMismo() {
        UUID usuarioId = UUID.randomUUID();

        assertThatThrownBy(() -> servicio.desactivar(new DesactivarUsuarioComando(usuarioId, usuarioId)))
                .isInstanceOf(AutodesactivacionException.class);

        verify(usuarioRepositoryPort, never()).guardar(any());
        verify(refreshTokenRepositoryPort, never()).revocarTodosDeUsuario(any(), any());
    }

    @Test
    void rechazaDesactivarAlUnicoAdministradorActivo() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolAdminId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(rolRepositoryPort.buscarPorNombre("ADMIN")).thenReturn(Optional.of(rolAdmin(rolAdminId)));
        when(usuarioRolRepositoryPort.existeAsignacion(usuarioId, rolAdminId)).thenReturn(true);
        when(usuarioRolRepositoryPort.contarUsuariosActivosConRol(rolAdminId)).thenReturn(1L);

        assertThatThrownBy(() -> servicio.desactivar(new DesactivarUsuarioComando(usuarioId, UUID.randomUUID())))
                .isInstanceOf(UltimoAdminException.class);

        verify(usuarioRepositoryPort, never()).guardar(any());
        verify(refreshTokenRepositoryPort, never()).revocarTodosDeUsuario(any(), any());
    }

    @Test
    void permiteDesactivarAUnAdministradorSiQuedaOtroActivo() {
        UUID usuarioId = UUID.randomUUID();
        UUID rolAdminId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario(usuarioId)));
        when(rolRepositoryPort.buscarPorNombre("ADMIN")).thenReturn(Optional.of(rolAdmin(rolAdminId)));
        when(usuarioRolRepositoryPort.existeAsignacion(usuarioId, rolAdminId)).thenReturn(true);
        when(usuarioRolRepositoryPort.contarUsuariosActivosConRol(rolAdminId)).thenReturn(2L);

        Usuario desactivado = servicio.desactivar(new DesactivarUsuarioComando(usuarioId, UUID.randomUUID()));

        assertThat(desactivado.getEstado()).isEqualTo(EstadoUsuario.INACTIVO);
    }

    @Test
    void rechazaDesactivarUnUsuarioInexistente() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.desactivar(new DesactivarUsuarioComando(usuarioId, UUID.randomUUID())))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void activaUnUsuarioInactivo() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId))
                .thenReturn(Optional.of(usuario(usuarioId, EstadoUsuario.INACTIVO)));

        assertThat(servicio.activar(usuarioId).getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    @Test
    void listaLosRolesDeUnUsuarioExistente() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuario(usuarioId)));
        Rol rol = Rol.reconstruir(UUID.randomUUID(), "OPERADOR", null, Instant.now());
        when(usuarioRolRepositoryPort.listarRolesDeUsuario(usuarioId)).thenReturn(List.of(rol));

        assertThat(servicio.listar(usuarioId)).containsExactly(rol);
    }

    @Test
    void rechazaListarRolesDeUnUsuarioInexistente() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.listar(usuarioId)).isInstanceOf(UsuarioNoEncontradoException.class);
    }
}
