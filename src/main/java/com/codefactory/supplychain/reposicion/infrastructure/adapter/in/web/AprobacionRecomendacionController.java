package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.ResolverRecomendacionUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.ResolverRecomendacionUseCase.Aprobacion;
import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.AprobacionRecomendacionResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.AprobarRecomendacionRequest;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.RecomendacionResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.OrdenReabastecimientoWebMapper;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.RecomendacionWebMapper;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Aprobar o rechazar recomendaciones de reposición al CD.
 * Scope {@code reposicion:planificar}; solo sobre las tiendas del usuario (fuera de alcance, 404).
 *
 * Módulo: reposicion.
 */
@RestController
@RequestMapping("/api/v1/recomendaciones")
@PreAuthorize("hasAuthority('reposicion:planificar')")
@Tag(name = "Recomendaciones de reposición")
public class AprobacionRecomendacionController {

    private final ResolverRecomendacionUseCase resolver;
    private final RecomendacionWebMapper recomendacionMapper;
    private final OrdenReabastecimientoWebMapper ordenMapper;

    public AprobacionRecomendacionController(ResolverRecomendacionUseCase resolver,
                                             RecomendacionWebMapper recomendacionMapper,
                                             OrdenReabastecimientoWebMapper ordenMapper) {
        this.resolver = resolver;
        this.recomendacionMapper = recomendacionMapper;
        this.ordenMapper = ordenMapper;
    }

    @PostMapping("/{id}/aprobar")
    @Operation(summary = "Aprobar una recomendación",
            description = "BORRADOR → APROBADA con la cantidad indicada o, sin cuerpo, con la sugerida. En la "
                    + "misma transacción crea la orden con fallback de CD: APROBADA con el primer CD que cubra "
                    + "toda la cantidad, o GENERADA sin CD con todos los intentos. Quien aprueba sale del token. "
                    + "HU-44.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false, content = @Content(
            examples = @ExampleObject(value = "{\"cantidadAprobada\": 15}")))
    @ApiResponse(responseCode = "201", description = "Recomendación aprobada y orden creada", content = @Content(
            schema = @Schema(implementation = AprobacionRecomendacionResponse.class),
            examples = @ExampleObject(value = """
                    {"recomendacionId": "6f1c2a4e-8b1d-4c7e-9a2f-3d5e7b9c1a20", "estado": "APROBADA",
                     "cantidadSugerida": 12, "cantidadAprobada": 15, "desviacion": 3,
                     "aprobadoPor": "3a4b5c6d-7e8f-4901-a2b3-c4d5e6f70812",
                     "fechaAprobacion": "2026-11-21T14:05:00Z",
                     "orden": {"id": "0d9e8f7a-6b5c-4d3e-8f21-a0b1c2d3e4f5",
                               "recomendacionId": "6f1c2a4e-8b1d-4c7e-9a2f-3d5e7b9c1a20",
                               "tiendaId": "1b2c3d4e-5f60-4718-8a9b-0c1d2e3f4a5b",
                               "varianteId": "9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4",
                               "cdAsignadoId": "c0ffee00-1234-4abc-9def-001122334455",
                               "motivoAsignacion": "Stock suficiente en el CD de prioridad 1",
                               "cantidadTotalSolicitada": 15, "cantidadPendiente": 15,
                               "cantidadDespachadaAcumulada": 0, "estado": "APROBADA",
                               "fechaCreacion": "2026-11-21T14:05:00Z",
                               "intentos": [{"cdId": "c0ffee00-1234-4abc-9def-001122334455",
                                             "ordenEvaluado": 1, "resultado": "ASIGNADO",
                                             "motivo": null, "timestamp": "2026-11-21T14:05:00Z"}]}}""")))
    @ApiResponse(responseCode = "400", description = "Cantidad menor o igual a cero, cuerpo ilegible o id mal "
            + "formado. No cambia nada")
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope reposicion:planificar")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda fuera del alcance del usuario")
    @ApiResponse(responseCode = "409", description = "La recomendación ya no está en BORRADOR. No crea otra orden")
    public ResponseEntity<AprobacionRecomendacionResponse> aprobar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @Parameter(example = "6f1c2a4e-8b1d-4c7e-9a2f-3d5e7b9c1a20") @PathVariable UUID id,
            @Valid @RequestBody(required = false) AprobarRecomendacionRequest request) {
        Integer cantidad = request == null ? null : request.cantidadAprobada();
        Aprobacion aprobacion = resolver.aprobar(usuario, id, cantidad);
        Recomendacion recomendacion = aprobacion.recomendacion();
        return ResponseEntity.status(HttpStatus.CREATED).body(new AprobacionRecomendacionResponse(
                recomendacion.getId(),
                recomendacion.getEstado(),
                recomendacion.getCantidadSugerida(),
                recomendacion.getCantidadAprobada(),
                recomendacion.desviacion().orElse(0),
                recomendacion.getAprobadoPor(),
                recomendacion.getFechaAprobacion(),
                ordenMapper.toResponse(aprobacion.orden())));
    }

    @PostMapping("/{id}/rechazar")
    @Operation(summary = "Rechazar una recomendación",
            description = "BORRADOR → RECHAZADA. No genera orden y el registro se conserva. HU-44.")
    @ApiResponse(responseCode = "200", description = "Recomendación rechazada", content = @Content(
            schema = @Schema(implementation = RecomendacionResponse.class),
            examples = @ExampleObject(value = """
                    {"id": "6f1c2a4e-8b1d-4c7e-9a2f-3d5e7b9c1a20",
                     "tiendaId": "1b2c3d4e-5f60-4718-8a9b-0c1d2e3f4a5b",
                     "varianteId": "9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4",
                     "cdSugeridoId": "c0ffee00-1234-4abc-9def-001122334455",
                     "cantidadSugerida": 12, "estado": "RECHAZADA",
                     "fechaCreacion": "2026-11-12T15:30:00Z"}""")))
    @ApiResponse(responseCode = "400", description = "El id no es un UUID válido")
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope reposicion:planificar")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda fuera del alcance del usuario")
    @ApiResponse(responseCode = "409", description = "La recomendación ya no está en BORRADOR")
    public RecomendacionResponse rechazar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @Parameter(example = "6f1c2a4e-8b1d-4c7e-9a2f-3d5e7b9c1a20") @PathVariable UUID id) {
        return recomendacionMapper.toResponse(resolver.rechazar(usuario, id));
    }
}
