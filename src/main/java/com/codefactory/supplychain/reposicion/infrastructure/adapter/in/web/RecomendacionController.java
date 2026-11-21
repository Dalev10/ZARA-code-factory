package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.ConsultarRecomendacionesUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.EvaluacionRecomendacionesUseCase;
import com.codefactory.supplychain.reposicion.domain.model.EstadoRecomendacion;
import com.codefactory.supplychain.reposicion.domain.model.Recomendacion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.EvaluacionRecomendacionesResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.RecomendacionResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.RecomendacionWebMapper;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Consulta y evaluación manual de recomendaciones de reposición al CD.
 * Scope {@code reposicion:planificar}; solo sobre las tiendas del usuario (fuera de alcance, 404).
 *
 * Módulo: reposicion.
 */
@RestController
@RequestMapping("/api/v1/recomendaciones")
@PreAuthorize("hasAuthority('reposicion:planificar')")
@Tag(name = "Recomendaciones de reposición", description = "Recomendaciones de reponer la Bodega_Tienda desde "
        + "un CD. Se generan solas cuando el pronosticado de la Bodega cae bajo el mínimo del nivel de reposición; "
        + "también se puede forzar una evaluación. Scope: reposicion:planificar.")
public class RecomendacionController {

    private final ConsultarRecomendacionesUseCase consultar;
    private final EvaluacionRecomendacionesUseCase evaluacion;
    private final RecomendacionWebMapper webMapper;

    public RecomendacionController(ConsultarRecomendacionesUseCase consultar,
                                   EvaluacionRecomendacionesUseCase evaluacion,
                                   RecomendacionWebMapper webMapper) {
        this.consultar = consultar;
        this.evaluacion = evaluacion;
        this.webMapper = webMapper;
    }

    @GetMapping
    @Operation(summary = "Listar recomendaciones",
            description = "Paginado, la más reciente primero. Filtros opcionales: estado, tienda y variante. "
                    + "Sin tiendaId, lista las de todas las tiendas del usuario. HU-43.")
    @ApiResponse(responseCode = "200", description = "Página de recomendaciones", content = @Content(
            examples = @ExampleObject(value = """
                    {"content": [{"id": "6f1c2a4e-8b1d-4c7e-9a2f-3d5e7b9c1a20",
                                  "tiendaId": "1b2c3d4e-5f60-4718-8a9b-0c1d2e3f4a5b",
                                  "varianteId": "9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4",
                                  "cdSugeridoId": "c0ffee00-1234-4abc-9def-001122334455",
                                  "cantidadSugerida": 12, "estado": "BORRADOR",
                                  "fechaCreacion": "2026-11-12T15:30:00Z"}],
                     "totalElements": 1, "totalPages": 1, "number": 0, "size": 20}""")))
    @ApiResponse(responseCode = "400", description = "Filtro con formato inválido (estado fuera de la lista o "
            + "UUID mal formado)")
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope reposicion:planificar")
    @ApiResponse(responseCode = "404", description = "La tienda filtrada está fuera del alcance del usuario")
    public ResponseEntity<Page<RecomendacionResponse>> listar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @Parameter(example = "BORRADOR") @RequestParam(required = false) EstadoRecomendacion estado,
            @Parameter(example = "1b2c3d4e-5f60-4718-8a9b-0c1d2e3f4a5b") @RequestParam(required = false) UUID tiendaId,
            @Parameter(example = "9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4") @RequestParam(required = false) UUID varianteId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(consultar.listar(usuario, estado, tiendaId, varianteId, pageable)
                .map(webMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una recomendación", description = "HU-43.")
    @ApiResponse(responseCode = "200", description = "Recomendación encontrada", content = @Content(
            schema = @Schema(implementation = RecomendacionResponse.class),
            examples = @ExampleObject(value = """
                    {"id": "6f1c2a4e-8b1d-4c7e-9a2f-3d5e7b9c1a20",
                     "tiendaId": "1b2c3d4e-5f60-4718-8a9b-0c1d2e3f4a5b",
                     "varianteId": "9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4",
                     "cdSugeridoId": "c0ffee00-1234-4abc-9def-001122334455",
                     "cantidadSugerida": 12, "estado": "BORRADOR",
                     "fechaCreacion": "2026-11-12T15:30:00Z"}""")))
    @ApiResponse(responseCode = "400", description = "El id no es un UUID válido")
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope reposicion:planificar")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda fuera del alcance del usuario")
    public RecomendacionResponse obtener(
            @AuthenticationPrincipal UsuarioActual usuario,
            @Parameter(example = "6f1c2a4e-8b1d-4c7e-9a2f-3d5e7b9c1a20") @PathVariable UUID id) {
        return webMapper.toResponse(consultar.obtener(usuario, id));
    }

    @PostMapping("/evaluar")
    @Operation(summary = "Evaluar recomendaciones a mano",
            description = "Evalúa cada variante con inventario de la tienda indicada o, sin tiendaId, de todas "
                    + "las tiendas activas del usuario. Crea una recomendación BORRADOR cuando el pronosticado de "
                    + "la Bodega está bajo el mínimo del nivel, con cantidad sugerida = objetivo − pronosticado. "
                    + "Idempotente: no duplica un BORRADOR ni recomienda si hay una orden GENERADA sin CD. HU-43.")
    @ApiResponse(responseCode = "200", description = "Recomendaciones creadas en esta evaluación (puede ser 0)",
            content = @Content(schema = @Schema(implementation = EvaluacionRecomendacionesResponse.class),
                    examples = @ExampleObject(value = """
                            {"recomendacionesCreadas": 1,
                             "recomendaciones": [{"id": "6f1c2a4e-8b1d-4c7e-9a2f-3d5e7b9c1a20",
                                                  "tiendaId": "1b2c3d4e-5f60-4718-8a9b-0c1d2e3f4a5b",
                                                  "varianteId": "9e8d7c6b-5a49-4382-b1a0-f9e8d7c6b5a4",
                                                  "cdSugeridoId": "c0ffee00-1234-4abc-9def-001122334455",
                                                  "cantidadSugerida": 12, "estado": "BORRADOR",
                                                  "fechaCreacion": "2026-11-12T15:30:00Z"}]}""")))
    @ApiResponse(responseCode = "400", description = "tiendaId no es un UUID válido")
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope reposicion:planificar")
    @ApiResponse(responseCode = "404", description = "La tienda está fuera del alcance del usuario")
    public ResponseEntity<EvaluacionRecomendacionesResponse> evaluar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @Parameter(description = "Tienda a evaluar; sin ella, todas las tiendas activas del usuario",
                    example = "1b2c3d4e-5f60-4718-8a9b-0c1d2e3f4a5b")
            @RequestParam(required = false) UUID tiendaId) {
        List<Recomendacion> creadas = evaluacion.evaluar(usuario, tiendaId);
        return ResponseEntity.ok(new EvaluacionRecomendacionesResponse(
                creadas.size(), creadas.stream().map(webMapper::toResponse).toList()));
    }
}
