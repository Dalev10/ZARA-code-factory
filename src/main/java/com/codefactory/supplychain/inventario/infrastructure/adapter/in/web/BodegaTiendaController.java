package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.BodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.BodegaTiendaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Solo lectura: la Bodega_Tienda se crea con su tienda (D-55).
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@RestController
@RequestMapping("/api/v1/bodegas-tienda")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('tiendas:administrar')")
@Tag(name = "Bodegas de tienda", description = "Solo lectura: cada Bodega se crea con su tienda. Scope: "
        + "tiendas:administrar.")
public class BodegaTiendaController {

    private final BodegaTiendaUseCase bodegaTiendaUseCase;

    @GetMapping
    @Operation(summary = "Listar bodegas de tienda", description = "Paginado. FEAT-03, HU-34.")
    @ApiResponse(responseCode = "200", description = "Página de bodegas")
    public Page<BodegaTiendaResponse> listarTodas(@ParameterObject Pageable pageable) {
        return bodegaTiendaUseCase.listarTodas(pageable).map(BodegaTiendaResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una bodega", description = "FEAT-03.")
    @ApiResponse(responseCode = "200", description = "Bodega encontrada")
    @ApiResponse(responseCode = "404", description = "Bodega inexistente")
    public BodegaTiendaResponse consultarPorId(@PathVariable UUID id) {
        return BodegaTiendaResponse.from(bodegaTiendaUseCase.consultarPorId(id));
    }

    @GetMapping("/tienda/{tiendaId}")
    @Operation(summary = "Obtener la bodega de una tienda", description = "FEAT-03, HU-34.")
    @ApiResponse(responseCode = "200", description = "Bodega de la tienda")
    @ApiResponse(responseCode = "404", description = "La tienda no existe o no tiene bodega")
    public BodegaTiendaResponse consultarPorTiendaId(@PathVariable UUID tiendaId) {
        return BodegaTiendaResponse.from(bodegaTiendaUseCase.consultarPorTiendaId(tiendaId));
    }
}
