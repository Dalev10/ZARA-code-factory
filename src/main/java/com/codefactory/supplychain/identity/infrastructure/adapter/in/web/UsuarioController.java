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
 * TODO(HU-11): el registro (POST) debe quedar restringido a un scope tipo
 * "usuarios:escribir" una vez exista el guard genérico de autorización. Hoy
 * queda abierto porque la autenticación (HU-03) y el guard (HU-11) todavía no
 * existen — ver SecurityConfig. Los endpoints agregados en HU-10 sí usan ya el
 * chequeo provisorio de "solo ADMIN" (AutorizacionAdmin) porque manejan datos y
 * asignaciones sensibles que no tiene sentido dejar abiertas mientras tanto.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
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
    @PreAuthorize("@autorizacionAdmin.esAdmin(authentication)")
    public List<UsuarioResponse> listar() {
        return listarUsuariosUseCase.listar().stream().map(UsuarioResponse::desde).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@autorizacionAdmin.esAdmin(authentication)")
    public UsuarioResponse obtener(@PathVariable UUID id) {
        return UsuarioResponse.desde(obtenerUsuarioUseCase.obtener(id));
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("@autorizacionAdmin.esAdmin(authentication)")
    public List<RolResponse> listarRoles(@PathVariable UUID id) {
        return listarRolesDeUsuarioUseCase.listar(id).stream().map(RolResponse::desde).toList();
    }

    @PostMapping("/{id}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@autorizacionAdmin.esAdmin(authentication)")
    public void asignarRol(@PathVariable UUID id, @Valid @RequestBody AsignarRolRequest request) {
        asignarRolAUsuarioUseCase.asignar(new AsignarRolComando(id, request.rolId()));
    }

    @DeleteMapping("/{id}/roles/{rolId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@autorizacionAdmin.esAdmin(authentication)")
    public void quitarRol(@PathVariable UUID id, @PathVariable UUID rolId) {
        quitarRolDeUsuarioUseCase.quitar(new AsignarRolComando(id, rolId));
    }
}
