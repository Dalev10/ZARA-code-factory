package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.controller;

import com.codefactory.supplychain.catalogo.application.port.in.CategoriaUseCase;
import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.CategoriaRequest;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.CategoriaResponse;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.mapper.CategoriaWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST para Categoria (FEAT-05 / HU-15 a HU-18).
 * <p>
 * Depende únicamente de {@link CategoriaUseCase}: no conoce el
 * {@code CategoriaService}, JPA, entidades ni adaptadores de
 * persistencia.
 */
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    private final CategoriaUseCase categoriaUseCase;
    private final CategoriaWebMapper categoriaWebMapper;

    public CategoriaController(CategoriaUseCase categoriaUseCase,
                                CategoriaWebMapper categoriaWebMapper) {
        this.categoriaUseCase = categoriaUseCase;
        this.categoriaWebMapper = categoriaWebMapper;
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        Categoria categoria = categoriaUseCase.crear(request.nombre());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoriaWebMapper.toResponse(categoria));
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listar() {
        List<CategoriaResponse> respuesta = categoriaUseCase.listar().stream()
                .map(categoriaWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> obtenerPorId(@PathVariable Long id) {
        Categoria categoria = categoriaUseCase.obtenerPorId(id);
        return ResponseEntity.ok(categoriaWebMapper.toResponse(categoria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> modificar(@PathVariable Long id,
                                                         @Valid @RequestBody CategoriaRequest request) {
        Categoria categoria = categoriaUseCase.modificar(id, request.nombre());
        return ResponseEntity.ok(categoriaWebMapper.toResponse(categoria));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
