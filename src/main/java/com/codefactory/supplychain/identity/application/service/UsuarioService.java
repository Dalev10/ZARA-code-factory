package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.ActivarUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.AsignarRolAUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.AsignarRolComando;
import com.codefactory.supplychain.identity.application.port.in.DesactivarUsuarioComando;
import com.codefactory.supplychain.identity.application.port.in.DesactivarUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.ListarRolesDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.ListarUsuariosUseCase;
import com.codefactory.supplychain.identity.application.port.in.ObtenerUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.QuitarRolDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.out.RefreshTokenRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRolRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.AutodesactivacionException;
import com.codefactory.supplychain.identity.domain.exception.RolNoEncontradoException;
import com.codefactory.supplychain.identity.domain.exception.UltimoAdminException;
import com.codefactory.supplychain.identity.domain.exception.UsuarioNoEncontradoException;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Concentra la lectura de Usuario (listar/obtener, complemento mínimo que le
 * faltaba a UsuarioController) y la asignación de Roles a Usuario (usuario_rol) —
 * mismo criterio de cohesión que RolService en HU-09. HU-31 suma desactivar/activar.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
@RequiredArgsConstructor
public class UsuarioService implements ListarUsuariosUseCase, ObtenerUsuarioUseCase, AsignarRolAUsuarioUseCase,
        QuitarRolDeUsuarioUseCase, ListarRolesDeUsuarioUseCase, DesactivarUsuarioUseCase, ActivarUsuarioUseCase {

    private static final String NOMBRE_ROL_ADMIN = "ADMIN";

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final RolRepositoryPort rolRepositoryPort;
    private final UsuarioRolRepositoryPort usuarioRolRepositoryPort;
    private final RefreshTokenRepositoryPort refreshTokenRepositoryPort;

    @Override
    public List<Usuario> listar() {
        return usuarioRepositoryPort.listarTodos();
    }

    @Override
    public Usuario obtener(UUID usuarioId) {
        return usuarioRepositoryPort.buscarPorId(usuarioId).orElseThrow(UsuarioNoEncontradoException::new);
    }

    @Override
    public void asignar(AsignarRolComando comando) {
        obtener(comando.usuarioId());
        Rol rol = rolRepositoryPort.buscarPorId(comando.rolId()).orElseThrow(RolNoEncontradoException::new);
        if (!usuarioRolRepositoryPort.existeAsignacion(comando.usuarioId(), rol.getId())) {
            usuarioRolRepositoryPort.asignar(comando.usuarioId(), rol.getId());
        }
    }

    @Override
    public void quitar(AsignarRolComando comando) {
        Rol rol = rolRepositoryPort.buscarPorId(comando.rolId()).orElseThrow(RolNoEncontradoException::new);
        if (NOMBRE_ROL_ADMIN.equals(rol.getNombre()) && usuarioRepositoryPort.buscarPorId(comando.usuarioId())
                .filter(usuario -> esElUltimoAdminActivo(usuario, rol)).isPresent()) {
            throw new UltimoAdminException();
        }
        usuarioRolRepositoryPort.quitar(comando.usuarioId(), rol.getId());
    }

    @Override
    public List<Rol> listar(UUID usuarioId) {
        obtener(usuarioId);
        return usuarioRolRepositoryPort.listarRolesDeUsuario(usuarioId);
    }

    /**
     * Se revocan sus refresh tokens en la misma transacción: si no, al reactivarlo, los
     * no vencidos resucitarían sesiones anteriores a la desactivación.
     */
    @Override
    @Transactional
    public Usuario desactivar(DesactivarUsuarioComando comando) {
        if (comando.usuarioId().equals(comando.actorId())) {
            throw new AutodesactivacionException();
        }
        Usuario usuario = obtener(comando.usuarioId());
        if (rolRepositoryPort.buscarPorNombre(NOMBRE_ROL_ADMIN)
                .filter(rolAdmin -> esElUltimoAdminActivo(usuario, rolAdmin)).isPresent()) {
            throw new UltimoAdminException();
        }
        Instant ahora = Instant.now();
        Usuario desactivado = usuarioRepositoryPort.guardar(usuario.desactivar(ahora));
        refreshTokenRepositoryPort.revocarTodosDeUsuario(usuario.getId(), ahora);
        return desactivado;
    }

    @Override
    public Usuario activar(UUID usuarioId) {
        return usuarioRepositoryPort.guardar(obtener(usuarioId).activar(Instant.now()));
    }

    /**
     * Quitarle el rol ADMIN o desactivar a este usuario dejaría al sistema sin ningún ADMIN
     * que pueda administrar. Un ADMIN ya inactivo no cuenta: perderlo no cambia nada.
     */
    private boolean esElUltimoAdminActivo(Usuario usuario, Rol rolAdmin) {
        return usuario.getEstado() == EstadoUsuario.ACTIVO
                && usuarioRolRepositoryPort.existeAsignacion(usuario.getId(), rolAdmin.getId())
                && usuarioRolRepositoryPort.contarUsuariosActivosConRol(rolAdmin.getId()) <= 1;
    }
}
