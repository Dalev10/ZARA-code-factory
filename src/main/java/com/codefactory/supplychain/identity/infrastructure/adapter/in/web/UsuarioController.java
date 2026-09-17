package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.in.AsignarRolAUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.AsignarRolComando;
import com.codefactory.supplychain.identity.application.port.in.ListarRolesDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.ListarUsuariosUseCase;
import com.codefactory.supplychain.identity.application.port.in.ObtenerUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.QuitarRolDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.RegistrarUsuarioComando;
import com.codefactory.supplychain.identity.application.port.in.RegistrarUsuarioUseCase;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.AsignarRolRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.RegistrarUsuarioRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.RolResponse;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Todo el controller exige el scope "usuarios:administrar" — incluido el
 * registro, que en HU-02 había quedado abierto (permitAll en SecurityConfig)
 * porque la autenticación y el guard genérico todavía no existían. Ahora que
 * ambos existen, se cierra para cumplir la decisión original: "admin crea
 * usuario, no self-signup".
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('usuarios:administrar')")
public class UsuarioController {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final ListarUsuariosUseCase listarUsuariosUseCase;
    private final ObtenerUsuarioUseCase obtenerUsuarioUseCase;
    private final AsignarRolAUsuarioUseCase asignarRolAUsuarioUseCase;
    private final QuitarRolDeUsuarioUseCase quitarRolDeUsuarioUseCase;
    private final ListarRolesDeUsuarioUseCase listarRolesDeUsuarioUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistrarUsuarioRequest request) {
        Usuario usuario = registrarUsuarioUseCase.registrar(
                new RegistrarUsuarioComando(request.email(), request.nombreCompleto(), request.password()));
        return UsuarioResponse.desde(usuario);
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return listarUsuariosUseCase.listar().stream().map(UsuarioResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable UUID id) {
        return UsuarioResponse.desde(obtenerUsuarioUseCase.obtener(id));
    }

    @GetMapping("/{id}/roles")
    public List<RolResponse> listarRoles(@PathVariable UUID id) {
        return listarRolesDeUsuarioUseCase.listar(id).stream().map(RolResponse::desde).toList();
    }

    @PostMapping("/{id}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asignarRol(@PathVariable UUID id, @Valid @RequestBody AsignarRolRequest request) {
        asignarRolAUsuarioUseCase.asignar(new AsignarRolComando(id, request.rolId()));
    }

    @DeleteMapping("/{id}/roles/{rolId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarRol(@PathVariable UUID id, @PathVariable UUID rolId) {
        quitarRolDeUsuarioUseCase.quitar(new AsignarRolComando(id, rolId));
    }
}
