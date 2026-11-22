package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.CerrarOrdenUseCase;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.MotivoCierreRequest;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.OrdenReabastecimientoResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.OrdenReabastecimientoWebMapper;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Cierre con faltante y cancelación de órdenes (HU-53). Scope {@code reposicion:planificar};
 * solo sobre las tiendas del usuario (fuera de alcance, 404).
 *
 * Módulo: reposicion.
 */
@RestController
@RequestMapping("/api/v1/ordenes-reabastecimiento")
@PreAuthorize("hasAuthority('reposicion:planificar')")
@RequiredArgsConstructor
@Tag(name = "Órdenes de reabastecimiento")
public class CierreOrdenController {

    private final CerrarOrdenUseCase cerrarOrden;
    private final OrdenReabastecimientoWebMapper mapper;

    @PostMapping("/{id}/cerrar")
    @Operation(summary = "Cerrar una orden con faltante",
            description = "La orden debe estar REABIERTA. Queda CERRADA aceptando lo que no llegó: el pendiente se "
                    + "conserva como registro y se liberan la reserva que quede en el CD (saliente) y el entrante "
                    + "pendiente de la Bodega. Motivo obligatorio. HU-53.")
    @ApiResponse(responseCode = "200", description = "Orden cerrada con faltante")
    @ApiResponse(responseCode = "400", description = "Motivo vacío o de más de 500 caracteres")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda fuera del alcance del usuario")
    @ApiResponse(responseCode = "409", description = "La orden no está REABIERTA")
    public OrdenReabastecimientoResponse cerrar(@AuthenticationPrincipal UsuarioActual usuario,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody MotivoCierreRequest request) {
        return mapper.toResponse(cerrarOrden.cerrarConFaltante(usuario, id, request.motivo()));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una orden sin CD",
            description = "La orden debe estar GENERADA (ningún CD tenía disponible). Queda CANCELADA; como nunca "
                    + "reservó, no cambia el inventario. Motivo obligatorio. HU-53.")
    @ApiResponse(responseCode = "200", description = "Orden cancelada")
    @ApiResponse(responseCode = "400", description = "Motivo vacío o de más de 500 caracteres")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda fuera del alcance del usuario")
    @ApiResponse(responseCode = "409", description = "La orden no está GENERADA")
    public OrdenReabastecimientoResponse cancelar(@AuthenticationPrincipal UsuarioActual usuario,
                                                  @PathVariable UUID id,
                                                  @Valid @RequestBody MotivoCierreRequest request) {
        return mapper.toResponse(cerrarOrden.cancelar(usuario, id, request.motivo()));
    }
}
