package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.UmbralReposicionUseCase;
import com.codefactory.supplychain.reposicion.domain.model.UmbralReposicion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.UmbralReposicionCantidadesRequest;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.UmbralReposicionRequest;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.UmbralReposicionResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.UmbralReposicionWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.UUID;

/**
 * Administración de umbrales de reposición interna (HU-47, D-57): protegida por el scope
 * {@code reposicion:administrar}.
 */
@RestController
@RequestMapping("/api/v1/umbrales-reposicion")
@PreAuthorize("hasAuthority('reposicion:administrar')")
@Tag(name = "Reposición interna", description = "Traspasos de la Bodega al Almacén de una tienda. Se "
        + "disparan solos cuando el Almacén queda por debajo del umbral mínimo; también se pueden registrar "
        + "a mano o forzar una evaluación. Scope: reposicion:administrar.")
public class UmbralReposicionController {

    private final UmbralReposicionUseCase umbralUseCase;
    private final UmbralReposicionWebMapper webMapper;

    public UmbralReposicionController(UmbralReposicionUseCase umbralUseCase,
                                      UmbralReposicionWebMapper webMapper) {
        this.umbralUseCase = umbralUseCase;
        this.webMapper = webMapper;
    }

    @PostMapping
    @Operation(summary = "Crear un umbral de reposición",
            description = "Aplica a una variante o a una categoría, nunca a ambas. El de la variante tiene "
                    + "precedencia sobre el de su categoría. La cantidad objetivo debe superar la mínima. HU-47.")
    @ApiResponse(responseCode = "201", description = "Umbral creado")
    @ApiResponse(responseCode = "400", description = "Sin variante ni categoría, con ambas, o cantidades inválidas")
    @ApiResponse(responseCode = "404", description = "Variante inexistente")
    @ApiResponse(responseCode = "409", description = "Ya existe un umbral para esa variante o categoría, o la "
            + "categoría no existe")
    public ResponseEntity<UmbralReposicionResponse> crear(@Valid @RequestBody UmbralReposicionRequest request) {
        UmbralReposicion umbral = umbralUseCase.crear(request.varianteId(), request.categoriaId(),
                request.cantidadMinima(), request.cantidadObjetivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(umbral));
    }

    @GetMapping
    @Operation(summary = "Listar umbrales de reposición", description = "Paginado. HU-47.")
    @ApiResponse(responseCode = "200", description = "Página de umbrales")
    public ResponseEntity<Page<UmbralReposicionResponse>> listar(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(umbralUseCase.listar(pageable).map(webMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un umbral de reposición", description = "HU-47.")
    @ApiResponse(responseCode = "200", description = "Umbral encontrado")
    @ApiResponse(responseCode = "404", description = "Umbral inexistente")
    public ResponseEntity<UmbralReposicionResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(webMapper.toResponse(umbralUseCase.obtenerPorId(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar las cantidades de un umbral",
            description = "Solo cambia mínimo y objetivo; la variante o categoría no se cambia. HU-47.")
    @ApiResponse(responseCode = "200", description = "Umbral modificado")
    @ApiResponse(responseCode = "400", description = "La cantidad objetivo no supera la mínima, o cuerpo inválido")
    @ApiResponse(responseCode = "404", description = "Umbral inexistente")
    public ResponseEntity<UmbralReposicionResponse> modificar(
            @PathVariable UUID id, @Valid @RequestBody UmbralReposicionCantidadesRequest request) {
        UmbralReposicion umbral = umbralUseCase.modificar(id, request.cantidadMinima(), request.cantidadObjetivo());
        return ResponseEntity.ok(webMapper.toResponse(umbral));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un umbral de reposición", description = "HU-47.")
    @ApiResponse(responseCode = "204", description = "Umbral eliminado")
    @ApiResponse(responseCode = "404", description = "Umbral inexistente")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        umbralUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
