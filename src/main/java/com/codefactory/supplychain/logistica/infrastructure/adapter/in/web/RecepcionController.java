package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web;

import com.codefactory.supplychain.logistica.application.port.in.ConsultarParadasPorRecibirUseCase;
import com.codefactory.supplychain.logistica.application.port.in.RegistrarRecepcionUseCase;
import com.codefactory.supplychain.logistica.application.port.in.RegistrarRecepcionUseCase.Comando;
import com.codefactory.supplychain.logistica.application.port.in.RegistrarRecepcionUseCase.ResultadoRecepcion;
import com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto.ParadaPorRecibirResponse;
import com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto.RecepcionResponse;
import com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto.RegistrarRecepcionRequest;
import com.codefactory.supplychain.shared.dto.ErrorResponse;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Recepción de las paradas en la Bodega_Tienda (HU-49). Scope {@code recepcion:registrar};
 * solo sobre las tiendas del usuario (fuera de alcance, 404).
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAuthority('recepcion:registrar')")
@RequiredArgsConstructor
@Tag(name = "Recepción", description = "Recepción en la Bodega_Tienda de lo que llega en cada parada de un viaje. "
        + "Suma lo recibido al inventario, registra la discrepancia como pérdida en tránsito y cierra o reabre "
        + "la orden de reabastecimiento. Scope: recepcion:registrar.")
public class RecepcionController {

    private static final String EJEMPLO_REGISTRAR = """
            {"cantidadRecibida": 8}""";

    private static final String EJEMPLO_RECEPCION = """
            {
              "id": "6f1c1b7e-2a4d-4b8e-9c1a-0d2e3f4a5b6c",
              "viajeParadaId": "1a2b3c4d-5e6f-4a1b-8c2d-3e4f5a6b7c8d",
              "cicloDespachoId": "9d8c7b6a-5f4e-4d3c-8b2a-1f0e9d8c7b6a",
              "cantidadDespachada": 10,
              "cantidadRecibida": 8,
              "discrepancia": 2,
              "registradoPor": "2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e",
              "fechaRecepcion": "2026-11-20T15:00:00Z",
              "estadoOrden": "REABIERTA",
              "cantidadPendienteOrden": 2
            }""";

    private final RegistrarRecepcionUseCase registrarRecepcion;
    private final ConsultarParadasPorRecibirUseCase consultarParadas;

    /** 201 la primera vez; 200 con la recepción original si la parada ya la tenía (D-91). */
    @PostMapping("/viajes/{viajeId}/paradas/{paradaId}/recepcion")
    @Operation(summary = "Registrar la recepción de una parada",
            description = "La parada debe estar ENTREGADO. Suma lo recibido a la Bodega_Tienda (crea la fila si la "
                    + "variante nunca estuvo), guarda la discrepancia (despachada − recibida) como pérdida en "
                    + "tránsito sin movimiento de kardex y registra lo recibido en la orden: queda CERRADA si no "
                    + "hay pendiente o REABIERTA si lo hay. Idempotente por parada: repetir el registro devuelve "
                    + "200 con la recepción original y no vuelve a sumar stock. HU-49.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(value = EJEMPLO_REGISTRAR)))
    @ApiResponse(responseCode = "201", description = "Recepción registrada",
            content = @Content(schema = @Schema(implementation = RecepcionResponse.class),
                    examples = @ExampleObject(value = EJEMPLO_RECEPCION)))
    @ApiResponse(responseCode = "200", description = "Reintento: la parada ya tenía recepción; se devuelve la original",
            content = @Content(schema = @Schema(implementation = RecepcionResponse.class)))
    @ApiResponse(responseCode = "400", description = "Cantidad recibida ausente o negativa, o cuerpo inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope recepcion:registrar")
    @ApiResponse(responseCode = "404", description = "La parada no existe, no es de ese viaje o su tienda está fuera "
            + "del alcance del usuario", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La parada no está ENTREGADO o la orden no está EN_TRANSITO",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "La cantidad recibida supera la despachada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<RecepcionResponse> registrar(@AuthenticationPrincipal UsuarioActual actor,
                                                       @PathVariable UUID viajeId,
                                                       @PathVariable UUID paradaId,
                                                       @Valid @RequestBody RegistrarRecepcionRequest request) {
        ResultadoRecepcion resultado = registrarRecepcion.registrar(
                new Comando(actor, viajeId, paradaId, request.cantidadRecibida()));
        return ResponseEntity.status(resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(RecepcionResponse.from(resultado));
    }

    @GetMapping("/paradas-por-recibir")
    @Operation(summary = "Listar las paradas por recibir",
            description = "Paradas ENTREGADO que todavía no tienen recepción, de las tiendas del usuario (o de todas "
                    + "con tiendas:operar-todas), ordenadas por viaje y orden de parada. HU-49.")
    @ApiResponse(responseCode = "200", description = "Paradas por recibir; lista vacía si no hay")
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope recepcion:registrar")
    public List<ParadaPorRecibirResponse> paradasPorRecibir(@AuthenticationPrincipal UsuarioActual actor) {
        return consultarParadas.listar(actor).stream().map(ParadaPorRecibirResponse::from).toList();
    }
}
