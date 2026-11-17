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
    @ApiResponse(responseCode = "200", description = "Página de recomendaciones")
    @ApiResponse(responseCode = "400", description = "Filtro con formato inválido")
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope reposicion:planificar")
    @ApiResponse(responseCode = "404", description = "La tienda filtrada está fuera del alcance del usuario")
    public ResponseEntity<Page<RecomendacionResponse>> listar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @Parameter(example = "BORRADOR") @RequestParam(required = false) EstadoRecomendacion estado,
            @RequestParam(required = false) UUID tiendaId,
            @RequestParam(required = false) UUID varianteId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(consultar.listar(usuario, estado, tiendaId, varianteId, pageable)
                .map(webMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una recomendación", description = "HU-43.")
    @ApiResponse(responseCode = "200", description = "Recomendación encontrada")
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope reposicion:planificar")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda fuera del alcance del usuario")
    public RecomendacionResponse obtener(@AuthenticationPrincipal UsuarioActual usuario, @PathVariable UUID id) {
        return webMapper.toResponse(consultar.obtener(usuario, id));
    }

    @PostMapping("/evaluar")
    @Operation(summary = "Evaluar recomendaciones a mano",
            description = "Evalúa cada variante con inventario de la tienda indicada o, sin tiendaId, de todas "
                    + "las tiendas activas del usuario. Crea una recomendación BORRADOR cuando el pronosticado de "
                    + "la Bodega está bajo el mínimo del nivel, con cantidad sugerida = objetivo − pronosticado. "
                    + "Idempotente: no duplica un BORRADOR ni recomienda si hay una orden GENERADA sin CD. HU-43.")
    @ApiResponse(responseCode = "200", description = "Recomendaciones creadas en esta evaluación (puede ser 0)")
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope reposicion:planificar")
    @ApiResponse(responseCode = "404", description = "La tienda está fuera del alcance del usuario")
    public ResponseEntity<EvaluacionRecomendacionesResponse> evaluar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @RequestParam(required = false) UUID tiendaId) {
        List<Recomendacion> creadas = evaluacion.evaluar(usuario, tiendaId);
        return ResponseEntity.ok(new EvaluacionRecomendacionesResponse(
                creadas.size(), creadas.stream().map(webMapper::toResponse).toList()));
    }
}
