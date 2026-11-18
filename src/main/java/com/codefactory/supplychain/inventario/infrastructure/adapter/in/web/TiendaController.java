package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.TiendaUseCase;
import com.codefactory.supplychain.inventario.domain.model.Tienda;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.TiendaRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.TiendaConNodosResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.TiendaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
 * ventas/inventario/etc. la referencian sin ON DELETE CASCADE. Crear y consultar
 * por id devuelven los ids de sus nodos Almacén y Bodega, necesarios para operar
 * su inventario.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@RestController
@RequestMapping("/api/v1/tiendas")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('tiendas:administrar')")
@Tag(name = "Tiendas", description = "Red de tiendas. Cada tienda nace con dos nodos de inventario: su Almacén "
        + "(piso de venta) y su Bodega.")
public class TiendaController {

    private final TiendaUseCase tiendaUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una tienda",
            description = "La crea ACTIVA junto con su Almacén y su Bodega y devuelve los ids de ambos nodos, que son "
                    + "los que usan las operaciones de inventario. Scope: tiendas:administrar. FEAT-02, HU-34.")
    @ApiResponse(responseCode = "201", description = "Tienda creada")
    @ApiResponse(responseCode = "409", description = "Ya existe una tienda con ese nombre")
    public TiendaConNodosResponse crear(@Valid @RequestBody TiendaRequest request) {
        return conNodos(tiendaUseCase.crear(request.nombre(), request.ubicacion()));
    }

    @GetMapping
    @Operation(summary = "Listar tiendas",
            description = "Activas e inactivas, paginadas. Scope: tiendas:administrar. FEAT-02.")
    @ApiResponse(responseCode = "200", description = "Página de tiendas")
    public Page<TiendaResponse> listar(@ParameterObject Pageable pageable) {
        return tiendaUseCase.listar(pageable).map(TiendaResponse::desde);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una tienda",
            description = "Incluye los ids de su Almacén y su Bodega. Scope: tiendas:administrar. FEAT-02.")
    @ApiResponse(responseCode = "200", description = "Tienda encontrada")
    @ApiResponse(responseCode = "404", description = "Tienda inexistente")
    public TiendaConNodosResponse obtener(@PathVariable UUID id) {
        return conNodos(tiendaUseCase.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una tienda",
            description = "Cambia nombre y ubicación. Scope: tiendas:administrar. FEAT-02.")
    @ApiResponse(responseCode = "200", description = "Tienda actualizada")
    @ApiResponse(responseCode = "404", description = "Tienda inexistente")
    @ApiResponse(responseCode = "409", description = "Ya existe otra tienda con ese nombre")
    public TiendaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody TiendaRequest request) {
        Tienda tienda = tiendaUseCase.actualizar(id, request.nombre(), request.ubicacion());
        return TiendaResponse.desde(tienda);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar una tienda",
            description = "Borrado lógico: la tienda pasa a INACTIVA, deja de admitir ventas y sale de la "
                    + "evaluación masiva de reposición; sus ventanas de quiebre abiertas se cierran con motivo "
                    + "TIENDA_DESACTIVADA. Su historial se "
                    + "conserva. Idempotente. Scope: tiendas:administrar. FEAT-02, HU-45.")
    @ApiResponse(responseCode = "204", description = "Tienda inactiva")
    @ApiResponse(responseCode = "404", description = "Tienda inexistente")
    public void desactivar(@PathVariable UUID id) {
        tiendaUseCase.desactivar(id);
    }

    @PutMapping("/{id}/activar")
    @Operation(summary = "Reactivar una tienda", description = "Scope: tiendas:administrar. HU-24.")
    @ApiResponse(responseCode = "200", description = "Tienda activa")
    @ApiResponse(responseCode = "404", description = "Tienda inexistente")
    public TiendaResponse activar(@PathVariable UUID id) {
        return TiendaResponse.desde(tiendaUseCase.activar(id));
    }

    private TiendaConNodosResponse conNodos(Tienda tienda) {
        return TiendaConNodosResponse.desde(tienda,
                tiendaUseCase.buscarAlmacen(tienda.getId()).orElse(null),
                tiendaUseCase.buscarBodega(tienda.getId()).orElse(null));
    }
}
