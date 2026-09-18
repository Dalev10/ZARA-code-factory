package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.in.ActualizarScopeComando;
import com.codefactory.supplychain.identity.application.port.in.ActualizarScopeUseCase;
import com.codefactory.supplychain.identity.application.port.in.CrearScopeComando;
import com.codefactory.supplychain.identity.application.port.in.CrearScopeUseCase;
import com.codefactory.supplychain.identity.application.port.in.EliminarScopeUseCase;
import com.codefactory.supplychain.identity.application.port.in.ListarScopesUseCase;
import com.codefactory.supplychain.identity.application.port.in.ObtenerScopeUseCase;
import com.codefactory.supplychain.identity.domain.model.Scope;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.ScopeRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.ScopeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@RestController
@RequestMapping("/api/v1/scopes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('scopes:administrar')")
public class ScopeController {

    private final CrearScopeUseCase crearScopeUseCase;
    private final ActualizarScopeUseCase actualizarScopeUseCase;
    private final EliminarScopeUseCase eliminarScopeUseCase;
    private final ListarScopesUseCase listarScopesUseCase;
    private final ObtenerScopeUseCase obtenerScopeUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScopeResponse crear(@Valid @RequestBody ScopeRequest request) {
        Scope scope = crearScopeUseCase.crear(
                new CrearScopeComando(request.codigo(), request.descripcion(), request.sensible()));
        return ScopeResponse.desde(scope);
    }

    @GetMapping
    public List<ScopeResponse> listar() {
        return listarScopesUseCase.listar().stream().map(ScopeResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public ScopeResponse obtener(@PathVariable UUID id) {
        return ScopeResponse.desde(obtenerScopeUseCase.obtener(id));
    }

    @PutMapping("/{id}")
    public ScopeResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ScopeRequest request) {
        Scope scope = actualizarScopeUseCase.actualizar(
                new ActualizarScopeComando(id, request.codigo(), request.descripcion(), request.sensible()));
        return ScopeResponse.desde(scope);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        eliminarScopeUseCase.eliminar(id);
    }
}
