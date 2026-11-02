package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.CentroDistribucionUseCase;
import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionConNodoResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.mapper.CentroDistribucionWebMapper;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@RestController
@RequestMapping("/api/v1/centros-distribucion")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('cd:administrar')")
@Tag(name = "Centros de distribución", description = "CD que abastecen a las tiendas. Cada CD nace con su nodo "
        + "de inventario. Scope: cd:administrar.")
public class CentroDistribucionController {

    private final CentroDistribucionUseCase centroDistribucionUseCase;
    private final CentroDistribucionWebMapper centroDistribucionWebMapper;

    @GetMapping
    @Operation(summary = "Buscar centros de distribución",
            description = "Lista paginada. Filtros opcionales: id exacto, y nombre o ubicación por coincidencia "
                    + "parcial sin distinguir mayúsculas. FEAT-04.")
    @ApiResponse(responseCode = "200", description = "Página de CD")
    public Page<CentroDistribucionResponse> obtenerCentroDistribucion(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String ubicacion,
            @ParameterObject Pageable pageable) {
        return centroDistribucionUseCase.buscarCentrosDistribucion(id, nombre, ubicacion, pageable)
                .map(centroDistribucionWebMapper::toResponse);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un centro de distribución",
            description = "Crea también su nodo de inventario. FEAT-04.")
    @ApiResponse(responseCode = "201", description = "CD creado")
    @ApiResponse(responseCode = "409", description = "Ya existe un CD con ese nombre")
    public CentroDistribucionResponse crearCentroDistribucion(
            @Valid @RequestBody CentroDistribucionRequest request) {
        CentroDistribucion centroDistribucion =
                centroDistribucionUseCase.crearCentroDistribucion(request.nombre(), request.ubicacion());
        return centroDistribucionWebMapper.toResponse(centroDistribucion);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un centro de distribución", description = "Cambia nombre y ubicación. FEAT-04.")
    @ApiResponse(responseCode = "200", description = "CD actualizado")
    @ApiResponse(responseCode = "404", description = "Centro de distribución inexistente")
    @ApiResponse(responseCode = "409", description = "Ya existe otro CD con ese nombre")
    public CentroDistribucionResponse actualizarCentroDistribucion(
            @PathVariable UUID id, @Valid @RequestBody CentroDistribucionRequest request) {
        CentroDistribucion centroDistribucion =
                centroDistribucionUseCase.actualizarCentroDistribucion(id, request.nombre(), request.ubicacion());
        return centroDistribucionWebMapper.toResponse(centroDistribucion);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un centro de distribución",
            description = "Borra el CD y su nodo. No se permite si alguna tienda lo tiene en su prioridad de "
                    + "abastecimiento o si su nodo tiene inventario o movimientos. FEAT-04, HU-22.")
    @ApiResponse(responseCode = "204", description = "CD eliminado")
    @ApiResponse(responseCode = "404", description = "Centro de distribución inexistente")
    @ApiResponse(responseCode = "409", description = "El CD está en la prioridad de alguna tienda o tiene inventario")
    public void eliminarCentroDistribucion(@PathVariable UUID id) {
        centroDistribucionUseCase.eliminarCentroDistribucion(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un centro de distribución", description = "Incluye su nodo de inventario. FEAT-04.")
    @ApiResponse(responseCode = "200", description = "CD encontrado")
    @ApiResponse(responseCode = "404", description = "Centro de distribución inexistente")
    public CentroDistribucionConNodoResponse obtenerCentroDistribucionConNodo(@PathVariable UUID id) {
        return centroDistribucionWebMapper.toResponse(centroDistribucionUseCase.buscarCentroDistribucionConNodo(id));
    }
}
