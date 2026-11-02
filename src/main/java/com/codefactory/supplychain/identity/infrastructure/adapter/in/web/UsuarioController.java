package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.in.ActivarUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.AsignarRolAUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.AsignarRolComando;
import com.codefactory.supplychain.identity.application.port.in.AsignarTiendaAUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.AsignarTiendaComando;
import com.codefactory.supplychain.identity.application.port.in.DesactivarUsuarioComando;
import com.codefactory.supplychain.identity.application.port.in.DesactivarUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.ListarRolesDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.ListarTiendasDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.ListarUsuariosUseCase;
import com.codefactory.supplychain.identity.application.port.in.ObtenerUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.QuitarRolDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.QuitarTiendaDeUsuarioUseCase;
import com.codefactory.supplychain.identity.application.port.in.RegistrarUsuarioComando;
import com.codefactory.supplychain.identity.application.port.in.RegistrarUsuarioUseCase;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.AsignarRolRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.AsignarTiendaRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.RegistrarUsuarioRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.RolResponse;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.TiendaAsignadaResponse;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.UsuarioResponse;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@Tag(name = "Usuarios", description = "Cuentas, sus roles y las tiendas en las que operan. Scope: "
        + "usuarios:administrar.")
public class UsuarioController {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;
    private final ListarUsuariosUseCase listarUsuariosUseCase;
    private final ObtenerUsuarioUseCase obtenerUsuarioUseCase;
    private final AsignarRolAUsuarioUseCase asignarRolAUsuarioUseCase;
    private final QuitarRolDeUsuarioUseCase quitarRolDeUsuarioUseCase;
    private final ListarRolesDeUsuarioUseCase listarRolesDeUsuarioUseCase;
    private final DesactivarUsuarioUseCase desactivarUsuarioUseCase;
    private final ActivarUsuarioUseCase activarUsuarioUseCase;
    private final AsignarTiendaAUsuarioUseCase asignarTiendaAUsuarioUseCase;
    private final QuitarTiendaDeUsuarioUseCase quitarTiendaDeUsuarioUseCase;
    private final ListarTiendasDeUsuarioUseCase listarTiendasDeUsuarioUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un usuario",
            description = "Crea la cuenta en estado ACTIVO, sin roles ni tiendas. La contraseña debe tener entre 12 "
                    + "y 128 caracteres y no figurar en filtraciones conocidas (Have I Been Pwned, consulta por "
                    + "k-anonimato: solo sale el prefijo del hash). El email no distingue mayúsculas. HU-02.")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Email, nombre o contraseña inválidos, o contraseña filtrada")
    @ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese email")
    public UsuarioResponse registrar(@Valid @RequestBody RegistrarUsuarioRequest request) {
        Usuario usuario = registrarUsuarioUseCase.registrar(
                new RegistrarUsuarioComando(request.email(), request.nombreCompleto(), request.password()));
        return UsuarioResponse.desde(usuario);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios",
            description = "Todos los usuarios, activos e inactivos. Sin paginación. HU-02.")
    public List<UsuarioResponse> listar() {
        return listarUsuariosUseCase.listar().stream().map(UsuarioResponse::desde).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un usuario", description = "HU-02.")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "404", description = "Usuario inexistente")
    public UsuarioResponse obtener(@PathVariable UUID id) {
        return UsuarioResponse.desde(obtenerUsuarioUseCase.obtener(id));
    }

    @GetMapping("/{id}/roles")
    @Operation(summary = "Listar los roles de un usuario", description = "Sin paginación. HU-10.")
    @ApiResponse(responseCode = "200", description = "Roles del usuario")
    @ApiResponse(responseCode = "404", description = "Usuario inexistente")
    public List<RolResponse> listarRoles(@PathVariable UUID id) {
        return listarRolesDeUsuarioUseCase.listar(id).stream().map(RolResponse::desde).toList();
    }

    @PostMapping("/{id}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Asignar un rol a un usuario",
            description = "Idempotente: si ya lo tenía, responde 204 igual. Los permisos nuevos rigen desde la "
                    + "siguiente petición del usuario, sin esperar a que renueve su token. HU-10.")
    @ApiResponse(responseCode = "204", description = "Rol asignado")
    @ApiResponse(responseCode = "404", description = "Usuario o rol inexistente")
    public void asignarRol(@PathVariable UUID id, @Valid @RequestBody AsignarRolRequest request) {
        asignarRolAUsuarioUseCase.asignar(new AsignarRolComando(id, request.rolId()));
    }

    @DeleteMapping("/{id}/roles/{rolId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quitar un rol a un usuario",
            description = "Idempotente: si no lo tenía, responde 204 igual. No permite quitar el rol ADMIN al último "
                    + "administrador activo. HU-10.")
    @ApiResponse(responseCode = "204", description = "Rol quitado")
    @ApiResponse(responseCode = "400", description = "Dejaría al sistema sin ningún ADMIN activo")
    @ApiResponse(responseCode = "404", description = "Rol inexistente")
    public void quitarRol(@PathVariable UUID id, @PathVariable UUID rolId) {
        quitarRolDeUsuarioUseCase.quitar(new AsignarRolComando(id, rolId));
    }

    // PUT y no DELETE: un usuario nunca se borra; solo deja de poder operar.
    @PutMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar un usuario",
            description = "El usuario deja de poder iniciar sesión y se revocan todos sus refresh tokens; sus "
                    + "access tokens vigentes dejan de aceptarse en la siguiente petición. Nadie puede "
                    + "desactivarse a sí mismo ni desactivar al último ADMIN activo. Los usuarios nunca se "
                    + "borran. HU-31.")
    @ApiResponse(responseCode = "200", description = "Usuario desactivado")
    @ApiResponse(responseCode = "400", description = "Es el propio usuario o el último ADMIN activo")
    @ApiResponse(responseCode = "404", description = "Usuario inexistente")
    public UsuarioResponse desactivar(@PathVariable UUID id, @AuthenticationPrincipal UsuarioActual actor) {
        return UsuarioResponse.desde(desactivarUsuarioUseCase.desactivar(new DesactivarUsuarioComando(id, actor.id())));
    }

    @PutMapping("/{id}/activar")
    @Operation(summary = "Reactivar un usuario",
            description = "Vuelve a ACTIVO. Las sesiones anteriores a la desactivación no reviven: debe iniciar "
                    + "sesión de nuevo. HU-31.")
    @ApiResponse(responseCode = "200", description = "Usuario activo")
    @ApiResponse(responseCode = "404", description = "Usuario inexistente")
    public UsuarioResponse activar(@PathVariable UUID id) {
        return UsuarioResponse.desde(activarUsuarioUseCase.activar(id));
    }

    @GetMapping("/{id}/tiendas")
    @Operation(summary = "Listar las tiendas de un usuario",
            description = "Tiendas en las que el usuario puede operar, con su estado. Sin paginación. HU-32.")
    @ApiResponse(responseCode = "200", description = "Tiendas del usuario")
    @ApiResponse(responseCode = "404", description = "Usuario inexistente")
    public List<TiendaAsignadaResponse> listarTiendas(@PathVariable UUID id) {
        return listarTiendasDeUsuarioUseCase.listarTiendas(id).stream().map(TiendaAsignadaResponse::desde).toList();
    }

    @PostMapping("/{id}/tiendas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Asignar una tienda a un usuario",
            description = "Idempotente. Se puede asignar una tienda inactiva: la asignación no habilita a operar en "
                    + "ella mientras siga inactiva. HU-32.")
    @ApiResponse(responseCode = "204", description = "Tienda asignada")
    @ApiResponse(responseCode = "404", description = "Usuario o tienda inexistente")
    public void asignarTienda(@PathVariable UUID id, @Valid @RequestBody AsignarTiendaRequest request) {
        asignarTiendaAUsuarioUseCase.asignar(new AsignarTiendaComando(id, request.tiendaId()));
    }

    @DeleteMapping("/{id}/tiendas/{tiendaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quitar una tienda a un usuario",
            description = "Idempotente: si no estaba asignada, o el usuario o la tienda no existen, responde "
                    + "204 igual. HU-32.")
    @ApiResponse(responseCode = "204", description = "Tienda quitada")
    public void quitarTienda(@PathVariable UUID id, @PathVariable UUID tiendaId) {
        quitarTiendaDeUsuarioUseCase.quitar(new AsignarTiendaComando(id, tiendaId));
    }
}
