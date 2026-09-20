package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.CentroDistribucionUseCase;
import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionConNodoResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CentroDistribucionResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.mapper.CentroDistribucionWebMapper;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@RestController
@RequestMapping("/api/v1/centros-distribucion")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('cd:administrar')")
public class CentroDistribucionController {

    private final CentroDistribucionUseCase centroDistribucionUseCase;
    private final CentroDistribucionWebMapper centroDistribucionWebMapper;

    @GetMapping
    public List<CentroDistribucionResponse> obtenerCentroDistribucion(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String ubicacion) {
        return centroDistribucionUseCase.buscarCentrosDistribucion(id, nombre, ubicacion).stream()
                .map(centroDistribucionWebMapper::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CentroDistribucionResponse crearCentroDistribucion(
            @Valid @RequestBody CentroDistribucionRequest request) {
        CentroDistribucion centroDistribucion =
                centroDistribucionUseCase.crearCentroDistribucion(request.nombre(), request.ubicacion());
        return centroDistribucionWebMapper.toResponse(centroDistribucion);
    }

    @PutMapping("/{id}")
    public CentroDistribucionResponse actualizarCentroDistribucion(
            @PathVariable UUID id, @Valid @RequestBody CentroDistribucionRequest request) {
        CentroDistribucion centroDistribucion =
                centroDistribucionUseCase.actualizarCentroDistribucion(id, request.nombre(), request.ubicacion());
        return centroDistribucionWebMapper.toResponse(centroDistribucion);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCentroDistribucion(@PathVariable UUID id) {
        centroDistribucionUseCase.eliminarCentroDistribucion(id);
    }

    @GetMapping("/{id}")
    public CentroDistribucionConNodoResponse obtenerCentroDistribucionConNodo(@PathVariable UUID id) {
        return centroDistribucionWebMapper.toResponse(centroDistribucionUseCase.buscarCentroDistribucionConNodo(id));
    }
}
