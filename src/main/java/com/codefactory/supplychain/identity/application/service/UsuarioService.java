package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.AsignarRolAUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.AsignarRolComando;
import com.codefactory.supplychain.identity.application.port.in.ListarRolesDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.ListarUsuariosUseCase;
import com.codefactory.supplychain.identity.application.port.in.ObtenerUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.QuitarRolDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRolRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.RolNoEncontradoException;
import com.codefactory.supplychain.identity.domain.exception.UltimoAdminException;
import com.codefactory.supplychain.identity.domain.exception.UsuarioNoEncontradoException;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Concentra la lectura de Usuario (listar/obtener, complemento mínimo que le
 * faltaba a UsuarioController) y la asignación de Roles a Usuario (usuario_rol) —
 * mismo criterio de cohesión que RolService en HU-09.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
@RequiredArgsConstructor
public class UsuarioService implements ListarUsuariosUseCase, ObtenerUsuarioUseCase, AsignarRolAUsuarioUseCase,
        QuitarRolDeUsuarioUseCase, ListarRolesDeUsuarioUseCase {

    private static final String NOMBRE_ROL_ADMIN = "ADMIN";

    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final RolRepositoryPort rolRepositoryPort;
    private final UsuarioRolRepositoryPort usuarioRolRepositoryPort;

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
        boolean estabaAsignado = usuarioRolRepositoryPort.existeAsignacion(comando.usuarioId(), rol.getId());
        if (estabaAsignado && NOMBRE_ROL_ADMIN.equals(rol.getNombre())
                && usuarioRolRepositoryPort.contarUsuariosConRol(rol.getId()) <= 1) {
            throw new UltimoAdminException();
        }
        usuarioRolRepositoryPort.quitar(comando.usuarioId(), rol.getId());
    }

    @Override
    public List<Rol> listar(UUID usuarioId) {
        obtener(usuarioId);
        return usuarioRolRepositoryPort.listarRolesDeUsuario(usuarioId);
    }
}
