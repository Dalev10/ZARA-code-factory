package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.in.RegistrarUsuarioComando;
import com.codefactory.supplychain.identity.application.port.in.RegistrarUsuarioUseCase;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.RegistrarUsuarioRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.UsuarioResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO(HU-11): este endpoint debe quedar restringido a un scope tipo "usuarios:escribir"
 * una vez exista el guard de autorización. Hoy queda abierto porque la autenticación
 * (HU-03) y el guard (HU-11) todavía no existen — ver SecurityConfig.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final RegistrarUsuarioUseCase registrarUsuarioUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistrarUsuarioRequest request) {
        Usuario usuario = registrarUsuarioUseCase.registrar(
                new RegistrarUsuarioComando(request.email(), request.nombreCompleto(), request.password()));
        return UsuarioResponse.desde(usuario);
    }
}
