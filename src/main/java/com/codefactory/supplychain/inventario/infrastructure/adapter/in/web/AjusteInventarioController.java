package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.AjustarInventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.in.AjusteInventarioComando;
import com.codefactory.supplychain.inventario.domain.model.MotivoAjuste;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.AjusteInventarioRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.AjusteInventarioResponse;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Ajuste de inventario de un nodo (HU-38).
 *
 * Igual que la carga inicial, el scope exacto (D-31) y el alcance por tienda
 * los resuelve el caso de uso, porque dependen del nodo.
 */
@RestController
@RequestMapping("/api/v1/nodos/{nodoId}/inventario")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('inventario:administrar', 'inventario-cd:administrar')")
public class AjusteInventarioController {

    private final AjustarInventarioUseCase ajustarInventarioUseCase;

    @PostMapping("/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registrar un ajuste de inventario",
            description = "Corrige una variante ya cargada en el nodo con un motivo cerrado: CONTEO_FISICO, "
                    + "INGRESO_MANUAL, MERMA o CORRECCION (esta última exige comentario). Nunca deja el "
                    + "stock en negativo. Registra el movimiento AJUSTE con el usuario del token.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ajuste registrado"),
            @ApiResponse(responseCode = "400", description = "Request inválido, motivo fuera de la lista, "
                    + "CORRECCION sin comentario, delta 0 o SKU inexistente"),
            @ApiResponse(responseCode = "401", description = "Token inválido o usuario inactivo"),
            @ApiResponse(responseCode = "403", description = "Sin el scope que exige el tipo de nodo"),
            @ApiResponse(responseCode = "404", description = "Nodo inexistente o fuera del alcance del usuario"),
            @ApiResponse(responseCode = "409", description = "Variante no cargada en el nodo "
                    + "(hoy responde 400, pendiente del cambio en shared)"),
            @ApiResponse(responseCode = "422", description = "El ajuste dejaría el stock en negativo "
                    + "(hoy responde 400, pendiente del cambio en shared)")
    })
    public AjusteInventarioResponse ajustar(
            @PathVariable UUID nodoId,
            @Valid @RequestBody AjusteInventarioRequest request,
            @AuthenticationPrincipal UsuarioActual usuario) {
        var movimiento = ajustarInventarioUseCase.ajustar(new AjusteInventarioComando(
                nodoId,
                request.sku(),
                request.delta(),
                MotivoAjuste.valueOf(request.motivo()),
                request.comentario(),
                usuario));
        return AjusteInventarioResponse.desde(nodoId, request.sku(), movimiento);
    }
}
