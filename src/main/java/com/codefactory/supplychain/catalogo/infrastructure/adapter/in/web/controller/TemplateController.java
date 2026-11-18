package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import com.codefactory.supplychain.catalogo.application.port.in.TemplateUseCase;
import com.codefactory.supplychain.catalogo.domain.model.Template;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.TemplateRequest;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.dto.TemplateResponse;
import com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.mapper.TemplateWebMapper;
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
@PreAuthorize("hasAuthority('catalogo:administrar')")
@Tag(name = "Catálogo", description = "Jerarquía de producto: Categoría > Template > Variante (SKU). La "
        + "Variante es la unidad que se vende y se almacena. Scope: catalogo:administrar.")
public class TemplateController {

    private final TemplateUseCase templateUseCase;
    private final TemplateWebMapper templateWebMapper;

    public TemplateController(TemplateUseCase templateUseCase,
                               TemplateWebMapper templateWebMapper) {
        this.templateUseCase = templateUseCase;
        this.templateWebMapper = templateWebMapper;
    }

    @PostMapping
    @Operation(summary = "Crear un template de producto",
            description = "Modelo de prenda (nombre, temporada, proveedor, precio base) dentro de una categoría "
                    + "obligatoria. FEAT-05, HU-35.")
    @ApiResponse(responseCode = "201", description = "Template creado")
    @ApiResponse(responseCode = "404", description = "Categoría inexistente")
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
    @Operation(summary = "Listar templates", description = "Paginado. FEAT-05.")
    @ApiResponse(responseCode = "200", description = "Página de templates")
    public ResponseEntity<Page<TemplateResponse>> listar(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(templateUseCase.listar(pageable).map(templateWebMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un template", description = "FEAT-05.")
    @ApiResponse(responseCode = "200", description = "Template encontrado")
    @ApiResponse(responseCode = "404", description = "Template inexistente")
    public ResponseEntity<TemplateResponse> obtenerPorId(@PathVariable UUID id) {
        Template template = templateUseCase.obtenerPorId(id);
        return ResponseEntity.ok(templateWebMapper.toResponse(template));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar un template",
            description = "Cambia nombre, temporada, proveedor y precio base. El cuerpo exige categoriaId, pero "
                    + "la categoría del template no cambia. FEAT-05.")
    @ApiResponse(responseCode = "200", description = "Template modificado")
    @ApiResponse(responseCode = "404", description = "Template inexistente")
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
    @Operation(summary = "Eliminar un template", description = "Solo si no tiene variantes. FEAT-05, HU-19.")
    @ApiResponse(responseCode = "204", description = "Template eliminado")
    @ApiResponse(responseCode = "404", description = "Template inexistente")
    @ApiResponse(responseCode = "409", description = "Tiene variantes asociadas")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        templateUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
