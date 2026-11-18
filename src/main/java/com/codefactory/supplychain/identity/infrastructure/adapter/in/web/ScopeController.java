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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Scopes", description = "Permisos atómicos (recurso:acción) que exige cada operación. Scope: "
        + "scopes:administrar.")
public class ScopeController {

    private final CrearScopeUseCase crearScopeUseCase;
    private final ActualizarScopeUseCase actualizarScopeUseCase;
    private final EliminarScopeUseCase eliminarScopeUseCase;
    private final ListarScopesUseCase listarScopesUseCase;
    private final ObtenerScopeUseCase obtenerScopeUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un scope",
            description = "Un scope nuevo no habilita nada hasta que una operación lo exija en el código. "
                    + "«sensible» es una marca informativa y por defecto es false. HU-09.")
    @ApiResponse(responseCode = "201", description = "Scope creado")
    @ApiResponse(responseCode = "409", description = "Ya existe un scope con ese código")
    public ScopeResponse crear(@Valid @RequestBody ScopeRequest request) {
        Scope scope = crearScopeUseCase.crear(
                new CrearScopeComando(request.codigo(), request.descripcion(), request.sensible()));
        return ScopeResponse.desde(scope);
    }

    @GetMapping
    @Operation(summary = "Listar scopes", description = "Sin paginación. HU-09.")
    public List<ScopeResponse> listar() {
        return listarScopesUseCase.listar().stream().map(ScopeResponse::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un scope", description = "HU-09.")
    @ApiResponse(responseCode = "200", description = "Scope encontrado")
    @ApiResponse(responseCode = "404", description = "Scope inexistente")
    public ScopeResponse obtener(@PathVariable UUID id) {
        return ScopeResponse.desde(obtenerScopeUseCase.obtener(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un scope",
            description = "Cambiar el código de un scope que el sistema exige deja esa operación sin nadie que la "
                    + "pueda ejecutar. HU-09.")
    @ApiResponse(responseCode = "200", description = "Scope actualizado")
    @ApiResponse(responseCode = "404", description = "Scope inexistente")
    @ApiResponse(responseCode = "409", description = "Ya existe otro scope con ese código")
    public ScopeResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ScopeRequest request) {
        Scope scope = actualizarScopeUseCase.actualizar(
                new ActualizarScopeComando(id, request.codigo(), request.descripcion(), request.sensible()));
        return ScopeResponse.desde(scope);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un scope", description = "Solo si ningún rol lo tiene asignado. HU-09.")
    @ApiResponse(responseCode = "204", description = "Scope eliminado")
    @ApiResponse(responseCode = "400", description = "El scope está asignado a uno o más roles")
    @ApiResponse(responseCode = "404", description = "Scope inexistente")
    public void eliminar(@PathVariable UUID id) {
        eliminarScopeUseCase.eliminar(id);
    }
}
