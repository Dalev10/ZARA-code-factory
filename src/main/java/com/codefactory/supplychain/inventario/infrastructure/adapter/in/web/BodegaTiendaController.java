package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.BodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.BodegaTiendaResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.ModificarBodegaTiendaRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.RegistrarBodegaTiendaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@RestController
@RequestMapping("/api/v1/bodegas-tienda")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('tiendas:administrar')")
public class BodegaTiendaController {

    private final BodegaTiendaUseCase bodegaTiendaUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BodegaTiendaResponse registrar(@Valid @RequestBody RegistrarBodegaTiendaRequest request) {
        return BodegaTiendaResponse.from(bodegaTiendaUseCase.registrar(request.tiendaId()));
    }

    @GetMapping
    public List<BodegaTiendaResponse> listarTodas() {
        return bodegaTiendaUseCase.listarTodas().stream().map(BodegaTiendaResponse::from).toList();
    }

    @GetMapping("/{id}")
    public BodegaTiendaResponse consultarPorId(@PathVariable UUID id) {
        return BodegaTiendaResponse.from(bodegaTiendaUseCase.consultarPorId(id));
    }

    @GetMapping("/tienda/{tiendaId}")
    public BodegaTiendaResponse consultarPorTiendaId(@PathVariable UUID tiendaId) {
        return BodegaTiendaResponse.from(bodegaTiendaUseCase.consultarPorTiendaId(tiendaId));
    }

    @PutMapping("/{id}")
    public BodegaTiendaResponse modificar(@PathVariable UUID id,
                                           @Valid @RequestBody ModificarBodegaTiendaRequest request) {
        return BodegaTiendaResponse.from(bodegaTiendaUseCase.modificar(id, request.tiendaId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        bodegaTiendaUseCase.eliminar(id);
    }
}
