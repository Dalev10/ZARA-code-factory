package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.controller;

import java.util.UUID;
import com.codefactory.supplychain.catalogo.application.port.in.TemplateUseCase;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.TemplateRequest;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.TemplateResponse;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.mapper.TemplateWebMapper;
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
 * Adaptador de entrada REST para Template (FEAT-05 / HU-15 a HU-18).
 * <p>
 * Depende únicamente de {@link TemplateUseCase}. El PUT respeta
 * exactamente el contrato existente de {@code TemplateUseCase#modificar}
 * (id, nombre, temporada, proveedor, precioBase): la categoría de un
 * Template no se puede reasignar, porque el UseCase actual no lo
 * permite (ver notas de dominio de Template).
 */
@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateUseCase templateUseCase;
    private final TemplateWebMapper templateWebMapper;

    public TemplateController(TemplateUseCase templateUseCase,
                               TemplateWebMapper templateWebMapper) {
        this.templateUseCase = templateUseCase;
        this.templateWebMapper = templateWebMapper;
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> crear(@Valid @RequestBody TemplateRequest request) {
        Template template = templateUseCase.crear(
                request.nombre(),
                request.temporada(),
                request.proveedor(),
                request.precioBase(),
                request.categoriaId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateWebMapper.toResponse(template));
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> listar() {
        List<TemplateResponse> respuesta = templateUseCase.listar().stream()
                .map(templateWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> obtenerPorId(@PathVariable UUID id) {
        Template template = templateUseCase.obtenerPorId(id);
        return ResponseEntity.ok(templateWebMapper.toResponse(template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> modificar(@PathVariable UUID id,
                                                        @Valid @RequestBody TemplateRequest request) {
        Template template = templateUseCase.modificar(
                id,
                request.nombre(),
                request.temporada(),
                request.proveedor(),
                request.precioBase()
        );
        return ResponseEntity.ok(templateWebMapper.toResponse(template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        templateUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
