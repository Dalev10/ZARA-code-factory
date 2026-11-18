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
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@Tag(name = "MFA", description = "Activación del segundo factor (TOTP) sobre la cuenta del usuario del token. "
        + "No exige scope.")
public class MfaController {

    private final ActivarMfaUseCase activarMfaUseCase;
    private final ConfirmarMfaUseCase confirmarMfaUseCase;

    @PostMapping("/activar")
    @Operation(
            summary = "Iniciar la activación de MFA",
            description = "Genera un secreto TOTP nuevo, lo guarda cifrado y lo devuelve en Base32 y como QR "
                    + "para registrarlo en una app autenticadora. MFA no se habilita hasta /confirmar; repetir "
                    + "esta llamada antes de confirmar reemplaza el secreto. HU-07.")
    @ApiResponse(responseCode = "200", description = "Secreto generado",
            content = @Content(schema = @Schema(implementation = ActivarMfaResponse.class),
                    examples = @ExampleObject(value = "{\"secreto\": \"JBSWY3DPEHPK3PXP\", "
                            + "\"qrCodeDataUri\": \"data:image/png;base64,iVBORw0KGgo...\"}")))
    @ApiResponse(responseCode = "400", description = "MFA ya está activo en la cuenta")
    public ActivarMfaResponse activar(@AuthenticationPrincipal UsuarioActual actor) {
        ActivarMfaResultado resultado = activarMfaUseCase.activar(new ActivarMfaComando(actor.id()));
        return new ActivarMfaResponse(resultado.secretoBase32(), resultado.qrCodeDataUri());
    }

    @PostMapping("/confirmar")
    @Operation(
            summary = "Confirmar la activación de MFA",
            description = "Verifica el primer código de 6 dígitos de la app autenticadora, habilita MFA y "
                    + "devuelve 8 códigos de respaldo de 10 caracteres y un solo uso. Se muestran solo esta "
                    + "vez. HU-07.")
    @ApiResponse(responseCode = "200", description = "MFA habilitado",
            content = @Content(schema = @Schema(implementation = ConfirmarMfaResponse.class),
                    examples = @ExampleObject(
                            value = "{\"codigosRespaldo\": [\"7KQ2M9XDPA\", \"P4LW3ZRTHN\", \"...\"]}")))
    @ApiResponse(responseCode = "400", description = "No se inició la activación, el código es incorrecto o el "
            + "cuerpo es inválido")
    public ConfirmarMfaResponse confirmar(@AuthenticationPrincipal UsuarioActual actor,
                                           @Valid @RequestBody ConfirmarMfaRequest request) {
        ConfirmarMfaResultado resultado = confirmarMfaUseCase.confirmar(
                new ConfirmarMfaComando(actor.id(), request.codigo()));
        return new ConfirmarMfaResponse(resultado.codigosRespaldo());
    }
}
