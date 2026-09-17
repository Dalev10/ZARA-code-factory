package com.codefactory.supplychain.identity.application.service;

import com.codefactory.supplychain.identity.application.port.in.ActualizarScopeComando;
import com.codefactory.supplychain.identity.application.port.in.ActualizarScopeUseCase;
import com.codefactory.supplychain.identity.application.port.in.CrearScopeComando;
import com.codefactory.supplychain.identity.application.port.in.CrearScopeUseCase;
import com.codefactory.supplychain.identity.application.port.in.EliminarScopeUseCase;
import com.codefactory.supplychain.identity.application.port.in.ListarScopesUseCase;
import com.codefactory.supplychain.identity.application.port.in.ObtenerScopeUseCase;
import com.codefactory.supplychain.identity.application.port.out.RolScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.ScopeRepositoryPort;
import com.codefactory.supplychain.identity.domain.exception.ScopeEnUsoException;
import com.codefactory.supplychain.identity.domain.exception.ScopeNoEncontradoException;
import com.codefactory.supplychain.identity.domain.exception.ScopeYaExistenteException;
import com.codefactory.supplychain.identity.domain.model.Scope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Service
@RequiredArgsConstructor
public class ScopeService implements CrearScopeUseCase, ActualizarScopeUseCase, EliminarScopeUseCase,
        ListarScopesUseCase, ObtenerScopeUseCase {

    private final ScopeRepositoryPort scopeRepositoryPort;
    private final RolScopeRepositoryPort rolScopeRepositoryPort;

    @Override
    public Scope crear(CrearScopeComando comando) {
        if (scopeRepositoryPort.existePorCodigo(comando.codigo())) {
            throw new ScopeYaExistenteException();
        }
        Scope scope = Scope.crear(comando.codigo(), comando.descripcion(), comando.sensible());
        return scopeRepositoryPort.guardar(scope);
    }

    @Override
    public Scope actualizar(ActualizarScopeComando comando) {
        Scope scope = obtener(comando.scopeId());
        if (!scope.getCodigo().equals(comando.codigo()) && scopeRepositoryPort.existePorCodigo(comando.codigo())) {
            throw new ScopeYaExistenteException();
        }
        Scope actualizado = scope.actualizar(comando.codigo(), comando.descripcion(), comando.sensible());
        return scopeRepositoryPort.guardar(actualizado);
    }

    @Override
    public void eliminar(UUID scopeId) {
        obtener(scopeId);
        if (rolScopeRepositoryPort.tieneRolesAsignados(scopeId)) {
            throw new ScopeEnUsoException();
        }
        scopeRepositoryPort.eliminar(scopeId);
    }

    @Override
    public List<Scope> listar() {
        return scopeRepositoryPort.listarTodos();
    }

    @Override
    public Scope obtener(UUID scopeId) {
        return scopeRepositoryPort.buscarPorId(scopeId).orElseThrow(ScopeNoEncontradoException::new);
    }
}
