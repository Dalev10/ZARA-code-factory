package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.NivelReposicionCdUseCase;
import com.codefactory.supplychain.reposicion.domain.model.NivelReposicionCd;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.NivelReposicionCdCantidadesRequest;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.NivelReposicionCdRequest;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.NivelReposicionCdResponse;
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
 * Administración de los niveles de reposición al CD (HD-04, D-74): protegida por el scope
 * {@code reposicion:administrar}.
 */
@RestController
@RequestMapping("/api/v1/niveles-reposicion-cd")
@PreAuthorize("hasAuthority('reposicion:administrar')")
@Tag(name = "Abastecimiento desde el CD", description = "Recomendaciones, órdenes de reabastecimiento y "
        + "niveles de reposición de la Bodega_Tienda frente al CD.")
public class NivelReposicionCdController {

    private final NivelReposicionCdUseCase nivelUseCase;

    public NivelReposicionCdController(NivelReposicionCdUseCase nivelUseCase) {
        this.nivelUseCase = nivelUseCase;
    }

    @PostMapping
    @Operation(summary = "Crear un nivel de reposición al CD",
            description = "Aplica a una variante o a una categoría, nunca a ambas. El de la variante tiene "
                    + "precedencia sobre el de su categoría. La cantidad objetivo debe superar la mínima. "
                    + "Se recomienda pedir al CD cuando el pronosticado de la Bodega queda bajo la mínima. HD-04.")
    @ApiResponse(responseCode = "201", description = "Nivel creado")
    @ApiResponse(responseCode = "400", description = "Sin variante ni categoría, con ambas, o cantidades inválidas")
    @ApiResponse(responseCode = "404", description = "Variante inexistente")
    @ApiResponse(responseCode = "409", description = "Ya existe un nivel para esa variante o categoría, o la "
            + "categoría no existe")
    public ResponseEntity<NivelReposicionCdResponse> crear(@Valid @RequestBody NivelReposicionCdRequest request) {
        NivelReposicionCd nivel = nivelUseCase.crear(request.varianteId(), request.categoriaId(),
                request.cantidadMinima(), request.cantidadObjetivo());
        return ResponseEntity.status(HttpStatus.CREATED).body(NivelReposicionCdResponse.desde(nivel));
    }

    @GetMapping
    @Operation(summary = "Listar niveles de reposición al CD", description = "Paginado. HD-04.")
    @ApiResponse(responseCode = "200", description = "Página de niveles")
    public ResponseEntity<Page<NivelReposicionCdResponse>> listar(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(nivelUseCase.listar(pageable).map(NivelReposicionCdResponse::desde));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un nivel de reposición al CD", description = "HD-04.")
    @ApiResponse(responseCode = "200", description = "Nivel encontrado")
    @ApiResponse(responseCode = "404", description = "Nivel inexistente")
    public ResponseEntity<NivelReposicionCdResponse> obtenerPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(NivelReposicionCdResponse.desde(nivelUseCase.obtenerPorId(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modificar las cantidades de un nivel de reposición al CD",
            description = "Solo cambia mínimo y objetivo; la variante o categoría no se cambia. HD-04.")
    @ApiResponse(responseCode = "200", description = "Nivel modificado")
    @ApiResponse(responseCode = "400", description = "La cantidad objetivo no supera la mínima, o cuerpo inválido")
    @ApiResponse(responseCode = "404", description = "Nivel inexistente")
    public ResponseEntity<NivelReposicionCdResponse> modificar(
            @PathVariable UUID id, @Valid @RequestBody NivelReposicionCdCantidadesRequest request) {
        NivelReposicionCd nivel = nivelUseCase.modificar(id, request.cantidadMinima(), request.cantidadObjetivo());
        return ResponseEntity.ok(NivelReposicionCdResponse.desde(nivel));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un nivel de reposición al CD", description = "HD-04.")
    @ApiResponse(responseCode = "204", description = "Nivel eliminado")
    @ApiResponse(responseCode = "404", description = "Nivel inexistente")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        nivelUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
