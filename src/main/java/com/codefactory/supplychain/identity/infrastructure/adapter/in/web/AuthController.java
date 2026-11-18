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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Autenticación", description = "Inicio y cierre de sesión. Públicos: la credencial es el cuerpo "
        + "o la cookie refresh_token (HttpOnly, Secure, SameSite=None, path /api/v1/auth).")
@SecurityRequirements
public class AuthController {

    private static final String COOKIE_REFRESH_TOKEN = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private static final String EJEMPLO_LOGIN_COMPLETO = """
            {"mfaRequerido": false, "mfaChallengeToken": null, "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
             "usuario": {"id": "3f8c2a1e-5b7d-4c9a-8e21-6d4f0b9a7c13", "email": "vendedor@supplychain.local",
                         "nombreCompleto": "Laura Gómez", "estado": "ACTIVO",
                         "creadoEn": "2026-10-01T14:30:00Z"}}
            """;
    private static final String EJEMPLO_LOGIN_MFA = """
            {"mfaRequerido": true, "mfaChallengeToken": "eyJhbGciOiJIUzI1NiJ9...", "accessToken": null,
             "usuario": null}
            """;

    private final LoginUseCase loginUseCase;
    private final CompletarLoginMfaUseCase completarLoginMfaUseCase;
    private final RefrescarTokenUseCase refrescarTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = "Valida email y contraseña. Sin MFA, devuelve el access token (JWT, 15 minutos) y "
                    + "deja el refresh token (7 días) en la cookie refresh_token. Con MFA, devuelve "
                    + "mfaRequerido=true y un token de desafío (5 minutos) para POST /api/v1/auth/login/mfa. "
                    + "Tras 3, 5, 7 y 10 intentos fallidos la cuenta se bloquea 1, 5, 15 y 60 minutos; "
                    + "durante el bloqueo responde 401 aunque la contraseña sea correcta. HU-03, HU-04.")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada, o segundo factor requerido",
            headers = @Header(name = "Set-Cookie", description = "refresh_token, solo si no se requiere MFA"),
            content = @Content(schema = @Schema(implementation = LoginResponse.class), examples = {
                    @ExampleObject(name = "Sesión iniciada", value = EJEMPLO_LOGIN_COMPLETO),
                    @ExampleObject(name = "Requiere MFA", value = EJEMPLO_LOGIN_MFA)}))
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas, usuario no activo o cuenta "
            + "bloqueada temporalmente. El mensaje es el mismo en los tres casos para no revelar cuál aplica")
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
    @Operation(
            summary = "Completar el inicio de sesión con MFA",
            description = "Cambia el token de desafío de /login y un código TOTP de 6 dígitos (o un código de "
                    + "respaldo, que se consume) por el access token y la cookie refresh_token. Un código "
                    + "incorrecto cuenta como intento fallido para el bloqueo progresivo de /login. HU-08.")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada",
            headers = @Header(name = "Set-Cookie", description = "refresh_token"),
            content = @Content(schema = @Schema(implementation = LoginResponse.class),
                    examples = @ExampleObject(name = "Sesión iniciada", value = EJEMPLO_LOGIN_COMPLETO)))
    @ApiResponse(responseCode = "400", description = "Código TOTP o de respaldo incorrecto, o cuerpo inválido")
    @ApiResponse(responseCode = "401", description = "Token de desafío inválido o vencido, usuario no "
            + "activo o cuenta bloqueada temporalmente")
    public LoginResponse completarLoginMfa(@Valid @RequestBody CompletarLoginMfaRequest request,
                                            HttpServletResponse response) {
        LoginResultado.Completado resultado = completarLoginMfaUseCase.completar(
                new CompletarLoginMfaComando(request.mfaChallengeToken(), request.codigo()));
        return construirRespuesta(resultado, response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar el access token",
            description = "Usa la cookie refresh_token, la revoca y emite una nueva (rotación). Presentar un "
                    + "refresh ya usado revoca todas las sesiones de esa familia y obliga a iniciar sesión de "
                    + "nuevo. Desde esta página el navegador envía la cookie que dejó /login. HU-05.")
    @ApiResponse(responseCode = "200", description = "Access token renovado",
            headers = @Header(name = "Set-Cookie", description = "Nuevo refresh_token"),
            content = @Content(schema = @Schema(implementation = RefrescarTokenResponse.class),
                    examples = @ExampleObject(value = "{\"accessToken\": \"eyJhbGciOiJIUzI1NiJ9...\"}")))
    @ApiResponse(responseCode = "401", description = "Sin cookie, refresh vencido, revocado o reusado, o "
            + "usuario no activo")
    public RefrescarTokenResponse refresh(
            @Parameter(description = "Refresh token emitido por /login, /login/mfa o /refresh")
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
    @Operation(
            summary = "Cerrar sesión",
            description = "Revoca el refresh token de la cookie y la borra. Siempre responde 204, haya o no "
                    + "sesión, para no revelar su estado. El access token sigue válido hasta que vence. HU-06.")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada",
            headers = @Header(name = "Set-Cookie", description = "refresh_token vacío con Max-Age=0"))
    public void logout(@Parameter(description = "Refresh token a revocar; opcional")
                        @CookieValue(name = COOKIE_REFRESH_TOKEN, required = false) String refreshTokenCookie,
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
