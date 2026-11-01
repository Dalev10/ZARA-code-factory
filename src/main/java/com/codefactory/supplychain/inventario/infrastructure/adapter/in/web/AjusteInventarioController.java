package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.AjustarInventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.in.AjusteInventarioComando;
import com.codefactory.supplychain.inventario.domain.model.MotivoAjuste;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.AjusteInventarioRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.AjusteInventarioResponse;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Inventario", description = "Existencias por nodo (CD, Almacén o Bodega de tienda), kardex, "
        + "carga inicial y ajustes. Las operaciones sobre un nodo de tienda exigen que el usuario opere en "
        + "ella; si no, el nodo se trata como inexistente (404).")
public class AjusteInventarioController {

    private static final String EJEMPLO_AJUSTE = """
            {"movimientoId": "0c2e4a6b-8d9f-4a1b-b2c3-d4e5f6a7b8c9", "nodoId": "4b6d8f0a-2c3e-4f5a-b6c7-d8e9f0a1b2c3",
             "sku": "CAM-OXF-AZ-M", "delta": -2, "motivo": "MERMA",
             "comentario": "Prendas manchadas en exhibición", "aLaMano": 38, "ocurridoEn": "2026-11-01T16:05:00Z"}
            """;

    private final AjustarInventarioUseCase ajustarInventarioUseCase;

    @PostMapping("/ajustes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registrar un ajuste de inventario",
            description = "Suma o resta delta al saldo de una variante en el nodo con un motivo cerrado: "
                    + "CONTEO_FISICO, INGRESO_MANUAL, MERMA o CORRECCION (esta última exige comentario). Nunca "
                    + "deja el stock en negativo. Si la variante no tenía fila en el nodo, un ajuste positivo la "
                    + "crea. Registra el movimiento AJUSTE con el usuario del token. Los CD exigen "
                    + "inventario-cd:administrar; las tiendas, inventario:administrar. HU-38.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(
            value = "{\"sku\": \"CAM-OXF-AZ-M\", \"delta\": -2, \"motivo\": \"MERMA\", "
                    + "\"comentario\": \"Prendas manchadas en exhibición\"}")))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ajuste registrado", content = @Content(
                    schema = @Schema(implementation = AjusteInventarioResponse.class),
                    examples = @ExampleObject(value = EJEMPLO_AJUSTE))),
            @ApiResponse(responseCode = "400", description = "Request inválido, motivo fuera de la lista, "
                    + "CORRECCION sin comentario, delta 0, SKU inexistente o el ajuste dejaría el stock en "
                    + "negativo"),
            @ApiResponse(responseCode = "401", description = "Token inválido o usuario inactivo"),
            @ApiResponse(responseCode = "403", description = "Sin el scope que exige el tipo de nodo"),
            @ApiResponse(responseCode = "404", description = "Nodo inexistente o fuera del alcance del usuario")
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
