package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.UmbralReposicionUseCase;
import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.UmbralReposicionCantidadesRequest;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.UmbralReposicionRequest;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.UmbralReposicionResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.UmbralReposicionWebMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Administración de umbrales de reposición interna (HU-38, D-57): protegida por el scope
 * {@code reposicion:administrar}.
 */
@RestController
@RequestMapping("/api/v1/umbrales-reposicion")
@PreAuthorize("hasAuthority('reposicion:administrar')")
public class UmbralReposicionController {

    private final UmbralReposicionUseCase umbralUseCase;
    private final UmbralReposicionWebMapper webMapper;

    public UmbralReposicionController(UmbralReposicionUseCase umbralUseCase,
                                      UmbralReposicionWebMapper webMapper) {
        this.umbralUseCase = umbralUseCase;
        this.webMapper = webMapper;
    }

    @PostMapping
    public ResponseEntity<UmbralReposicionResponse> crear(@Valid @RequestBody UmbralReposicionRequest request) {
        UmbralReposicion umbral = umbralUseCase.crear(request.varianteId(), request.categoriaId(),
                request.cantidadMinima(), request.cantidadObjetivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(umbral));
    }

    @GetMapping
    public ResponseEntity<Page<UmbralReposicionResponse>> listar(Pageable pageable) {
        return ResponseEntity.ok(umbralUseCase.listar(pageable).map(webMapper::toResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UmbralReposicionResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(webMapper.toResponse(umbralUseCase.obtenerPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UmbralReposicionResponse> modificar(
            @PathVariable UUID id, @Valid @RequestBody UmbralReposicionCantidadesRequest request) {
        UmbralReposicion umbral = umbralUseCase.modificar(id, request.cantidadMinima(), request.cantidadObjetivo());
        return ResponseEntity.ok(webMapper.toResponse(umbral));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        umbralUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
