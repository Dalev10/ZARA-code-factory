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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('roles:administrar')")
@Tag(name = "Roles", description = "Roles y los scopes que otorgan. Scope: roles:administrar.")
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
    @Operation(summary = "Crear un rol", description = "Nace sin scopes. HU-09.")
    @ApiResponse(responseCode = "201", description = "Rol creado")
    @ApiResponse(responseCode = "409", description = "Ya existe un rol con ese nombre")
    public RolResponse crear(@Valid @RequestBody RolRequest request) {
        Rol rol = crearRolUseCase.crear(new CrearRolComando(request.nombre(), request.descripcion()));
        return RolResponse.desde(rol);
    }

    @GetMapping
    @Operation(summary = "Listar roles", description = "Sin paginación. HU-09.")
    public List<RolResponse> listar() {
        return listarRolesUseCase.listar().stream().map(RolResponse::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un rol", description = "HU-09.")
    @ApiResponse(responseCode = "200", description = "Rol encontrado")
    @ApiResponse(responseCode = "404", description = "Rol inexistente")
    public RolResponse obtener(@PathVariable UUID id) {
        return RolResponse.desde(obtenerRolUseCase.obtener(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un rol", description = "Cambia nombre y descripción. HU-09.")
    @ApiResponse(responseCode = "200", description = "Rol actualizado")
    @ApiResponse(responseCode = "404", description = "Rol inexistente")
    @ApiResponse(responseCode = "409", description = "Ya existe otro rol con ese nombre")
    public RolResponse actualizar(@PathVariable UUID id, @Valid @RequestBody RolRequest request) {
        Rol rol = actualizarRolUseCase.actualizar(new ActualizarRolComando(id, request.nombre(), request.descripcion()));
        return RolResponse.desde(rol);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un rol", description = "Solo si ningún usuario lo tiene asignado. HU-09.")
    @ApiResponse(responseCode = "204", description = "Rol eliminado")
    @ApiResponse(responseCode = "400", description = "El rol tiene usuarios asignados")
    @ApiResponse(responseCode = "404", description = "Rol inexistente")
    public void eliminar(@PathVariable UUID id) {
        eliminarRolUseCase.eliminar(id);
    }

    @GetMapping("/{id}/scopes")
    @Operation(summary = "Listar los scopes de un rol", description = "Sin paginación. HU-09, HU-11.")
    @ApiResponse(responseCode = "200", description = "Scopes del rol")
    @ApiResponse(responseCode = "404", description = "Rol inexistente")
    public List<ScopeResponse> listarScopes(@PathVariable UUID id) {
        return listarScopesDeRolUseCase.listar(id).stream().map(ScopeResponse::desde).toList();
    }

    @PostMapping("/{id}/scopes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Asignar un scope a un rol",
            description = "Idempotente. Los usuarios con el rol obtienen el permiso desde su siguiente petición. "
                    + "HU-09, HU-11.")
    @ApiResponse(responseCode = "204", description = "Scope asignado")
    @ApiResponse(responseCode = "404", description = "Rol o scope inexistente")
    public void asignarScope(@PathVariable UUID id, @Valid @RequestBody AsignarScopeRequest request) {
        asignarScopeARolUseCase.asignar(new AsignarScopeComando(id, request.scopeId()));
    }

    @DeleteMapping("/{id}/scopes/{scopeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quitar un scope a un rol",
            description = "Idempotente: si no estaba asignado, o el rol o el scope no existen, responde 204 igual. "
                    + "HU-09, HU-11.")
    @ApiResponse(responseCode = "204", description = "Scope quitado")
    public void quitarScope(@PathVariable UUID id, @PathVariable UUID scopeId) {
        quitarScopeDeRolUseCase.quitar(new AsignarScopeComando(id, scopeId));
    }
}
