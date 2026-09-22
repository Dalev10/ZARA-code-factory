package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.in.CompletarLoginMfaComando;
import com.codefactory.supplychain.identity.application.port.in.CompletarLoginMfaUseCase;
import com.codefactory.supplychain.identity.application.port.in.LoginComando;
import com.codefactory.supplychain.identity.application.port.in.LoginResultado;
import com.codefactory.supplychain.identity.application.port.in.LoginUseCase;
import com.codefactory.supplychain.identity.application.port.in.LogoutComando;
import com.codefactory.supplychain.identity.application.port.in.LogoutUseCase;
import com.codefactory.supplychain.identity.application.port.in.RefrescarTokenComando;
import com.codefactory.supplychain.identity.application.port.in.RefrescarTokenResultado;
import com.codefactory.supplychain.identity.application.port.in.RefrescarTokenUseCase;
import com.codefactory.supplychain.identity.domain.exception.TokenInvalidoException;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.CompletarLoginMfaRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.LoginRequest;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.LoginResponse;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.RefrescarTokenResponse;
import com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto.UsuarioResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    private final CompletarLoginMfaUseCase completarLoginMfaUseCase;
    private final RefrescarTokenUseCase refrescarTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResultado resultado = loginUseCase.login(new LoginComando(request.email(), request.password()));
        return construirRespuesta(resultado, response);
    }

    /**
     * Segundo paso del login cuando la cuenta tiene MFA habilitado: se intercambia
     * el token de desafío (recibido en la respuesta de /login) más un código TOTP
     * o de respaldo por los tokens reales de sesión.
     */
    @PostMapping("/login/mfa")
    public LoginResponse completarLoginMfa(@Valid @RequestBody CompletarLoginMfaRequest request,
                                            HttpServletResponse response) {
        LoginResultado.Completado resultado = completarLoginMfaUseCase.completar(
                new CompletarLoginMfaComando(request.mfaChallengeToken(), request.codigo()));
        return construirRespuesta(resultado, response);
    }

    @PostMapping("/refresh")
    public RefrescarTokenResponse refresh(
            @CookieValue(name = COOKIE_REFRESH_TOKEN, required = false) String refreshTokenCookie,
            HttpServletResponse response) {
        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            throw new TokenInvalidoException();
        }

        RefrescarTokenResultado resultado = refrescarTokenUseCase.refrescar(
                new RefrescarTokenComando(refreshTokenCookie));

        response.addHeader(HttpHeaders.SET_COOKIE,
                construirCookieRefreshToken(resultado.nuevoRefreshTokenValor(), resultado.nuevoRefreshTokenExpiraEn())
                        .toString());

        return new RefrescarTokenResponse(resultado.accessToken());
    }

    /**
     * Idempotente a propósito: sin cookie, con un token ya inválido, o con uno
     * vigente, siempre responde 204 y limpia la cookie — nunca revela si había
     * o no una sesión activa.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = COOKIE_REFRESH_TOKEN, required = false) String refreshTokenCookie,
                        HttpServletResponse response) {
        if (refreshTokenCookie != null && !refreshTokenCookie.isBlank()) {
            logoutUseCase.logout(new LogoutComando(refreshTokenCookie));
        }

        response.addHeader(HttpHeaders.SET_COOKIE, construirCookieDeBorrado().toString());
    }

    private static LoginResponse construirRespuesta(LoginResultado resultado, HttpServletResponse response) {
        return switch (resultado) {
            case LoginResultado.RequiereMfa requiereMfa -> LoginResponse.requiereMfa(requiereMfa.mfaChallengeToken());
            case LoginResultado.Completado completado -> {
                response.addHeader(HttpHeaders.SET_COOKIE,
                        construirCookieRefreshToken(completado.refreshTokenValor(), completado.refreshTokenExpiraEn())
                                .toString());
                yield LoginResponse.completado(completado.accessToken(), UsuarioResponse.desde(completado.usuario()));
            }
        };
    }

    private static ResponseCookie construirCookieRefreshToken(String valor, Instant expiraEn) {
        Duration maxAge = Duration.between(Instant.now(), expiraEn);
        return ResponseCookie.from(COOKIE_REFRESH_TOKEN, valor)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    private static ResponseCookie construirCookieDeBorrado() {
        return ResponseCookie.from(COOKIE_REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}
