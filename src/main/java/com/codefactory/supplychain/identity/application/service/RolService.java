package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.ActualizarRolComando;
import com.codefactory.supplychain.identity.application.port.in.ActualizarRolUseCase;
import com.codefactory.supplychain.identity.application.port.in.AsignarScopeARolUseCase;
import com.codefactory.supplychain.identity.application.port.in.AsignarScopeComando;
import com.codefactory.supplychain.identity.application.port.in.CrearRolComando;
import com.codefactory.supplychain.identity.application.port.in.CrearRolUseCase;
import com.codefactory.supplychain.identity.application.port.in.EliminarRolUseCase;
import com.codefactory.supplychain.identity.application.port.in.ListarRolesUseCase;
import com.codefactory.supplychain.identity.application.port.in.ListarScopesDeRolUseCase;
import com.codefactory.supplychain.identity.application.port.in.ObtenerRolUseCase;
import com.codefactory.supplychain.identity.application.port.in.QuitarScopeDeRolUseCase;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Concentra el CRUD de Rol y la gestión de sus Scopes asignados (rol_scope) —
 * son operaciones que comparten el mismo agregado y las mismas dependencias, así
 * que separarlas en un servicio por caso de uso solo agregaría archivos sin
 * aportar cohesión adicional.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
@RequiredArgsConstructor
public class RolService implements CrearRolUseCase, ActualizarRolUseCase, EliminarRolUseCase, ListarRolesUseCase,
        ObtenerRolUseCase, AsignarScopeARolUseCase, QuitarScopeDeRolUseCase, ListarScopesDeRolUseCase {

    private final RolRepositoryPort rolRepositoryPort;
    private final ScopeRepositoryPort scopeRepositoryPort;
    private final RolScopeRepositoryPort rolScopeRepositoryPort;
    private final UsuarioRolRepositoryPort usuarioRolRepositoryPort;

    @Override
    public Rol crear(CrearRolComando comando) {
        if (rolRepositoryPort.existePorNombre(comando.nombre())) {
            throw new RolYaExistenteException();
        }
        Rol rol = Rol.crear(comando.nombre(), comando.descripcion());
        return rolRepositoryPort.guardar(rol);
    }

    @Override
    public Rol actualizar(ActualizarRolComando comando) {
        Rol rol = obtener(comando.rolId());
        if (!rol.getNombre().equals(comando.nombre()) && rolRepositoryPort.existePorNombre(comando.nombre())) {
            throw new RolYaExistenteException();
        }
        Rol actualizado = rol.actualizar(comando.nombre(), comando.descripcion());
        return rolRepositoryPort.guardar(actualizado);
    }

    @Override
    public void eliminar(UUID rolId) {
        obtener(rolId);
        if (usuarioRolRepositoryPort.tieneUsuariosAsignados(rolId)) {
            throw new RolEnUsoException();
        }
        rolRepositoryPort.eliminar(rolId);
    }

    @Override
    public List<Rol> listar() {
        return rolRepositoryPort.listarTodos();
    }

    @Override
    public Rol obtener(UUID rolId) {
        return rolRepositoryPort.buscarPorId(rolId).orElseThrow(RolNoEncontradoException::new);
    }

    @Override
    public void asignar(AsignarScopeComando comando) {
        obtener(comando.rolId());
        if (scopeRepositoryPort.buscarPorId(comando.scopeId()).isEmpty()) {
            throw new ScopeNoEncontradoException();
        }
        if (!rolScopeRepositoryPort.existeAsignacion(comando.rolId(), comando.scopeId())) {
            rolScopeRepositoryPort.asignar(comando.rolId(), comando.scopeId());
        }
    }

    @Override
    public void quitar(AsignarScopeComando comando) {
        rolScopeRepositoryPort.quitar(comando.rolId(), comando.scopeId());
    }

    @Override
    public List<Scope> listar(UUID rolId) {
        obtener(rolId);
        return rolScopeRepositoryPort.listarScopesDeRol(rolId);
    }
}
