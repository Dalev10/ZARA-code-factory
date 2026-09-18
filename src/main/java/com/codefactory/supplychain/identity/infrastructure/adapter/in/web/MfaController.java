package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.in.ActivarMfaComando;
import com.codefactory.supplychain.identity.application.port.in.ActivarMfaResultado;
import com.codefactory.supplychain.identity.application.port.in.ActivarMfaUseCase;
import com.codefactory.supplychain.identity.application.port.in.ConfirmarMfaComando;
import com.codefactory.supplychain.identity.application.port.in.ConfirmarMfaResultado;
import com.codefactory.supplychain.identity.application.port.in.ConfirmarMfaUseCase;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.ActivarMfaResponse;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.ConfirmarMfaRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.ConfirmarMfaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Requiere un JWT válido (ver SecurityConfig / JwtAuthenticationFilter): activar MFA
 * es una acción de un usuario autenticado sobre su propia cuenta, nunca sobre la de
 * otro — el usuarioId sale del token, nunca del request.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@RestController
@RequestMapping("/api/v1/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final ActivarMfaUseCase activarMfaUseCase;
    private final ConfirmarMfaUseCase confirmarMfaUseCase;

    @PostMapping("/activar")
    public ActivarMfaResponse activar(Authentication authentication) {
        ActivarMfaResultado resultado = activarMfaUseCase.activar(
                new ActivarMfaComando(usuarioIdDe(authentication)));
        return new ActivarMfaResponse(resultado.secretoBase32(), resultado.qrCodeDataUri());
    }

    @PostMapping("/confirmar")
    public ConfirmarMfaResponse confirmar(Authentication authentication,
                                           @Valid @RequestBody ConfirmarMfaRequest request) {
        ConfirmarMfaResultado resultado = confirmarMfaUseCase.confirmar(
                new ConfirmarMfaComando(usuarioIdDe(authentication), request.codigo()));
        return new ConfirmarMfaResponse(resultado.codigosRespaldo());
    }

    private static UUID usuarioIdDe(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
