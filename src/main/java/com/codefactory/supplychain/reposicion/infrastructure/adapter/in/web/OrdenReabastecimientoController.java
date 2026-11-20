package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.ConsultarOrdenesUseCase;
import com.codefactory.supplychain.reposicion.domain.model.EstadoOrden;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.OrdenReabastecimientoResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.OrdenReabastecimientoWebMapper;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Consulta de órdenes de reabastecimiento con sus intentos de CD. La creación
 * ocurre al aprobar una recomendación. Scope {@code reposicion:planificar};
 * solo sobre las tiendas del usuario (fuera de alcance, 404).
 *
 * Módulo: reposicion.
 */
@RestController
@RequestMapping("/api/v1/ordenes-reabastecimiento")
@PreAuthorize("hasAuthority('reposicion:planificar')")
@Tag(name = "Órdenes de reabastecimiento", description = "Órdenes CD → Bodega_Tienda creadas al aprobar una "
        + "recomendación. Se asigna el primer CD de la prioridad de la tienda que cubra toda la cantidad; si "
        + "ninguno la cubre, la orden queda GENERADA sin CD. Scope: reposicion:planificar.")
public class OrdenReabastecimientoController {

    private final ConsultarOrdenesUseCase consultar;
    private final OrdenReabastecimientoWebMapper webMapper;

    public OrdenReabastecimientoController(ConsultarOrdenesUseCase consultar,
                                           OrdenReabastecimientoWebMapper webMapper) {
        this.consultar = consultar;
        this.webMapper = webMapper;
    }

    @GetMapping
    @Operation(summary = "Listar órdenes de reabastecimiento",
            description = "Paginado, la más reciente primero, con sus intentos de CD. Filtros opcionales: "
                    + "estado y tienda. Sin tiendaId, lista las de todas las tiendas del usuario. HU-45.")
    @ApiResponse(responseCode = "200", description = "Página de órdenes")
    @ApiResponse(responseCode = "400", description = "Filtro con formato inválido")
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope reposicion:planificar")
    @ApiResponse(responseCode = "404", description = "La tienda filtrada está fuera del alcance del usuario")
    public ResponseEntity<Page<OrdenReabastecimientoResponse>> listar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @Parameter(example = "APROBADA") @RequestParam(required = false) EstadoOrden estado,
            @RequestParam(required = false) UUID tiendaId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(consultar.listar(usuario, estado, tiendaId, pageable).map(webMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una orden con sus intentos de CD", description = "HU-45.")
    @ApiResponse(responseCode = "200", description = "Orden encontrada")
    @ApiResponse(responseCode = "401", description = "Sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin el scope reposicion:planificar")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda fuera del alcance del usuario")
    public OrdenReabastecimientoResponse obtener(@AuthenticationPrincipal UsuarioActual usuario,
                                                 @PathVariable UUID id) {
        return webMapper.toResponse(consultar.obtener(usuario, id));
    }
}
