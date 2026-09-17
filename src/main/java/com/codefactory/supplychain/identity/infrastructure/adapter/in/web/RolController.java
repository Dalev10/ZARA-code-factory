package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

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
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.AsignarScopeRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.RolRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.RolResponse;
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
 * Restringido a ADMIN vía un chequeo provisorio (ver AutorizacionAdmin en 'shared')
 * hasta que exista el guard genérico de autorización por scope de HU-11.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("@autorizacionAdmin.esAdmin(authentication)")
public class RolController {

    private final CrearRolUseCase crearRolUseCase;
    private final ActualizarRolUseCase actualizarRolUseCase;
    private final EliminarRolUseCase eliminarRolUseCase;
    private final ListarRolesUseCase listarRolesUseCase;
    private final ObtenerRolUseCase obtenerRolUseCase;
    private final AsignarScopeARolUseCase asignarScopeARolUseCase;
    private final QuitarScopeDeRolUseCase quitarScopeDeRolUseCase;
    private final ListarScopesDeRolUseCase listarScopesDeRolUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolResponse crear(@Valid @RequestBody RolRequest request) {
        Rol rol = crearRolUseCase.crear(new CrearRolComando(request.nombre(), request.descripcion()));
        return RolResponse.desde(rol);
    }

    @GetMapping
    public List<RolResponse> listar() {
        return listarRolesUseCase.listar().stream().map(RolResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public RolResponse obtener(@PathVariable UUID id) {
        return RolResponse.desde(obtenerRolUseCase.obtener(id));
    }

    @PutMapping("/{id}")
    public RolResponse actualizar(@PathVariable UUID id, @Valid @RequestBody RolRequest request) {
        Rol rol = actualizarRolUseCase.actualizar(new ActualizarRolComando(id, request.nombre(), request.descripcion()));
        return RolResponse.desde(rol);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        eliminarRolUseCase.eliminar(id);
    }

    @GetMapping("/{id}/scopes")
    public List<ScopeResponse> listarScopes(@PathVariable UUID id) {
        return listarScopesDeRolUseCase.listar(id).stream().map(ScopeResponse::desde).toList();
    }

    @PostMapping("/{id}/scopes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asignarScope(@PathVariable UUID id, @Valid @RequestBody AsignarScopeRequest request) {
        asignarScopeARolUseCase.asignar(new AsignarScopeComando(id, request.scopeId()));
    }

    @DeleteMapping("/{id}/scopes/{scopeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarScope(@PathVariable UUID id, @PathVariable UUID scopeId) {
        quitarScopeDeRolUseCase.quitar(new AsignarScopeComando(id, scopeId));
    }
}
