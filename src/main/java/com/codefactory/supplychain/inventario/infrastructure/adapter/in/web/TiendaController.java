package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.TiendaUseCase;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.TiendaRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.TiendaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import java.util.UUID;

/**
 * "Eliminar" una tienda es desactivarla (estado), nunca un DELETE físico —
 * ventas/inventario/etc. la referencian sin ON DELETE CASCADE.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@RestController
@RequestMapping("/api/v1/tiendas")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('tiendas:administrar')")
public class TiendaController {

    private final TiendaUseCase tiendaUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TiendaResponse crear(@Valid @RequestBody TiendaRequest request) {
        Tienda tienda = tiendaUseCase.crear(request.nombre(), request.ubicacion());
        return TiendaResponse.desde(tienda);
    }

    @GetMapping
    public Page<TiendaResponse> listar(Pageable pageable) {
        return tiendaUseCase.listar(pageable).map(TiendaResponse::desde);
    }

    @GetMapping("/{id}")
    public TiendaResponse obtener(@PathVariable UUID id) {
        return TiendaResponse.desde(tiendaUseCase.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public TiendaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody TiendaRequest request) {
        Tienda tienda = tiendaUseCase.actualizar(id, request.nombre(), request.ubicacion());
        return TiendaResponse.desde(tienda);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable UUID id) {
        tiendaUseCase.desactivar(id);
    }

    @PutMapping("/{id}/activar")
    public TiendaResponse activar(@PathVariable UUID id) {
        return TiendaResponse.desde(tiendaUseCase.activar(id));
    }
}
