package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.BodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.BodegaTiendaResponse;
import lombok.RequiredArgsConstructor;
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
public class BodegaTiendaController {

    private final BodegaTiendaUseCase bodegaTiendaUseCase;

    @GetMapping
    public Page<BodegaTiendaResponse> listarTodas(Pageable pageable) {
        return bodegaTiendaUseCase.listarTodas(pageable).map(BodegaTiendaResponse::from);
    }

    @GetMapping("/{id}")
    public BodegaTiendaResponse consultarPorId(@PathVariable UUID id) {
        return BodegaTiendaResponse.from(bodegaTiendaUseCase.consultarPorId(id));
    }

    @GetMapping("/tienda/{tiendaId}")
    public BodegaTiendaResponse consultarPorTiendaId(@PathVariable UUID tiendaId) {
        return BodegaTiendaResponse.from(bodegaTiendaUseCase.consultarPorTiendaId(tiendaId));
    }
}
