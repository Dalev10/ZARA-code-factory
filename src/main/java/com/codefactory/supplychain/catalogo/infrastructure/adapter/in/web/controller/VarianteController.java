package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import com.codefactory.supplychain.catalogo.application.port.in.VarianteUseCase;
import com.codefactory.supplychain.catalogo.domain.model.Variante;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.VarianteRequest;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.VarianteResponse;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.mapper.VarianteWebMapper;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Catálogo", description = "Jerarquía de producto: Categoría > Template > Variante (SKU). La "
        + "Variante es la unidad que se vende y se almacena. Scope: catalogo:administrar.")
public class VarianteController {

    private final VarianteUseCase varianteUseCase;
    private final VarianteWebMapper varianteWebMapper;

    public VarianteController(VarianteUseCase varianteUseCase,
                               VarianteWebMapper varianteWebMapper) {
        this.varianteUseCase = varianteUseCase;
        this.varianteWebMapper = varianteWebMapper;
    }

    @PostMapping
    @Operation(summary = "Crear una variante",
            description = "Combinación de talla y color de un template, identificada por un SKU único. FEAT-05.")
    @ApiResponse(responseCode = "201", description = "Variante creada")
    @ApiResponse(responseCode = "404", description = "Template inexistente")
    @ApiResponse(responseCode = "409", description = "Ya existe una variante con ese SKU")
    public ResponseEntity<VarianteResponse> crear(@Valid @RequestBody VarianteRequest request) {
        Variante variante = varianteUseCase.crear(
                request.sku(), request.templateId(), request.talla(), request.color());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(varianteWebMapper.toResponse(variante));
    }

    @GetMapping
    @Operation(summary = "Listar variantes", description = "Paginado. FEAT-05.")
    @ApiResponse(responseCode = "200", description = "Página de variantes")
    public ResponseEntity<Page<VarianteResponse>> listar(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(varianteUseCase.listar(pageable).map(varianteWebMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una variante", description = "FEAT-05.")
    @ApiResponse(responseCode = "200", description = "Variante encontrada")
    @ApiResponse(responseCode = "404", description = "Variante inexistente")
    public ResponseEntity<VarianteResponse> obtenerPorId(@PathVariable UUID id) {
        Variante variante = varianteUseCase.obtenerPorId(id);
        return ResponseEntity.ok(varianteWebMapper.toResponse(variante));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Obtener una variante por SKU", description = "FEAT-05.")
    @ApiResponse(responseCode = "200", description = "Variante encontrada")
    @ApiResponse(responseCode = "404", description = "No hay variante con ese SKU")
    public ResponseEntity<VarianteResponse> obtenerPorSku(@PathVariable String sku) {
        Variante variante = varianteUseCase.obtenerPorSku(sku);
        return ResponseEntity.ok(varianteWebMapper.toResponse(variante));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar una variante",
            description = "Cambia SKU, template, talla y color. FEAT-05.")
    @ApiResponse(responseCode = "200", description = "Variante modificada")
    @ApiResponse(responseCode = "404", description = "Variante o template inexistente")
    @ApiResponse(responseCode = "409", description = "El SKU nuevo ya lo usa otra variante")
    public ResponseEntity<VarianteResponse> modificar(@PathVariable UUID id,
                                                        @Valid @RequestBody VarianteRequest request) {
        Variante variante = varianteUseCase.modificar(
                id, request.sku(), request.templateId(), request.talla(), request.color());
        return ResponseEntity.ok(varianteWebMapper.toResponse(variante));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una variante",
            description = "Solo si no tiene inventario, movimientos, ventas, solicitudes ni umbrales; con historial "
                    + "la base la rechaza. FEAT-05.")
    @ApiResponse(responseCode = "204", description = "Variante eliminada")
    @ApiResponse(responseCode = "404", description = "Variante inexistente")
    @ApiResponse(responseCode = "409", description = "Tiene inventario o historial asociado")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        varianteUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
