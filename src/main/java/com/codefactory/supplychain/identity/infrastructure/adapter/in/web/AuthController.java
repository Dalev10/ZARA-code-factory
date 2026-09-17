package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.in.LoginComando;
import com.codefactory.supplychain.identity.application.port.in.LoginResultado;
import com.codefactory.supplychain.identity.application.port.in.LoginUseCase;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.LoginRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.LoginResponse;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.UsuarioResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String COOKIE_REFRESH_TOKEN = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final LoginUseCase loginUseCase;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResultado resultado = loginUseCase.login(new LoginComando(request.email(), request.password()));

        response.addHeader(HttpHeaders.SET_COOKIE, construirCookieRefreshToken(resultado).toString());

        return new LoginResponse(resultado.accessToken(), UsuarioResponse.desde(resultado.usuario()));
    }

    private static ResponseCookie construirCookieRefreshToken(LoginResultado resultado) {
        Duration maxAge = Duration.between(Instant.now(), resultado.refreshTokenExpiraEn());
        return ResponseCookie.from(COOKIE_REFRESH_TOKEN, resultado.refreshTokenValor())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
