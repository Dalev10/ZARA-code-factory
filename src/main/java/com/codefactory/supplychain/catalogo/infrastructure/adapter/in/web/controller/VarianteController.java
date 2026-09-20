package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.controller;

import java.util.UUID;
import com.codefactory.supplychain.catalogo.application.port.in.VarianteUseCase;
import com.codefactory.supplychain.catalogo.domain.model.Variante;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.VarianteRequest;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.VarianteResponse;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.mapper.VarianteWebMapper;
import jakarta.validation.Valid;
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

import java.util.List;

/**
 * Adaptador de entrada REST para Variante (FEAT-05 / HU-15 a HU-18).
 * <p>
 * Depende únicamente de {@link VarianteUseCase}. El PUT respeta
 * exactamente el contrato existente de
 * {@code VarianteUseCase#modificar(id, nuevoSku, nuevoTemplateId, nuevaTalla, nuevoColor)}.
 */
@RestController
@RequestMapping("/api/v1/variantes")
@PreAuthorize("hasAuthority('catalogo:administrar')")
public class VarianteController {

    private final VarianteUseCase varianteUseCase;
    private final VarianteWebMapper varianteWebMapper;

    public VarianteController(VarianteUseCase varianteUseCase,
                               VarianteWebMapper varianteWebMapper) {
        this.varianteUseCase = varianteUseCase;
        this.varianteWebMapper = varianteWebMapper;
    }

    @PostMapping
    public ResponseEntity<VarianteResponse> crear(@Valid @RequestBody VarianteRequest request) {
        Variante variante = varianteUseCase.crear(
                request.sku(), request.templateId(), request.talla(), request.color());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(varianteWebMapper.toResponse(variante));
    }

    @GetMapping
    public ResponseEntity<List<VarianteResponse>> listar() {
        List<VarianteResponse> respuesta = varianteUseCase.listar().stream()
                .map(varianteWebMapper::toResponse)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VarianteResponse> obtenerPorId(@PathVariable UUID id) {
        Variante variante = varianteUseCase.obtenerPorId(id);
        return ResponseEntity.ok(varianteWebMapper.toResponse(variante));
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<VarianteResponse> obtenerPorSku(@PathVariable String sku) {
        Variante variante = varianteUseCase.obtenerPorSku(sku);
        return ResponseEntity.ok(varianteWebMapper.toResponse(variante));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VarianteResponse> modificar(@PathVariable UUID id,
                                                        @Valid @RequestBody VarianteRequest request) {
        Variante variante = varianteUseCase.modificar(
                id, request.sku(), request.templateId(), request.talla(), request.color());
        return ResponseEntity.ok(varianteWebMapper.toResponse(variante));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        varianteUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
