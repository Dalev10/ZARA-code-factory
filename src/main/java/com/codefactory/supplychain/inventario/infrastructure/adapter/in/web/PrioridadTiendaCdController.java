package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.PrioridadTiendaCdUseCase;
import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.PrioridadTiendaCdRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.PrioridadTiendaCdResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.mapper.PrioridadTiendaCdWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * HU-51: prioridad de CDs para una tienda.
 * <p>
 * PUT reemplaza la lista completa. El UNIQUE DEFERRABLE de V23 garantiza que
 * reordenar no viola unicidad en pasos intermedios del mismo flush.
 * <p>
 * Scope: {@code inventario:administrar} — lo tiene SUPERVISOR_TIENDA y ADMIN.
 */
@RestController
@RequestMapping("/api/v1/tiendas/{tiendaId}/prioridad-cd")
@PreAuthorize("hasAuthority('inventario:administrar')")
@Tag(name = "Tiendas")
public class PrioridadTiendaCdController {

    private final PrioridadTiendaCdUseCase useCase;
    private final PrioridadTiendaCdWebMapper webMapper;

    public PrioridadTiendaCdController(PrioridadTiendaCdUseCase useCase,
                                       PrioridadTiendaCdWebMapper webMapper) {
        this.useCase = useCase;
        this.webMapper = webMapper;
    }

    @PutMapping
    @Operation(summary = "Reemplazar la prioridad de CD de una tienda",
            description = "Recibe la lista completa de CD en orden (el primero es la prioridad 1) y reemplaza la "
                    + "anterior; una lista vacía la borra. Scope: inventario:administrar. HU-51.")
    @ApiResponse(responseCode = "200", description = "Prioridad guardada, en orden")
    @ApiResponse(responseCode = "409", description = "CD repetido en la lista, o tienda o CD inexistente (lo "
            + "rechazan las restricciones de la base)")
    public ResponseEntity<List<PrioridadTiendaCdResponse>> reemplazar(
            @PathVariable UUID tiendaId,
            @Valid @RequestBody PrioridadTiendaCdRequest request) {

        List<PrioridadTiendaCd> prioridades = useCase.reemplazar(tiendaId, request.cdIds());
        return ResponseEntity.ok(prioridades.stream().map(webMapper::toResponse).toList());
    }

    @GetMapping
    @Operation(summary = "Consultar la prioridad de CD de una tienda",
            description = "Lista ordenada; vacía si no tiene. Sin paginación. Scope: inventario:administrar. HU-51.")
    @ApiResponse(responseCode = "200", description = "CD en orden de prioridad")
    public ResponseEntity<List<PrioridadTiendaCdResponse>> listar(@PathVariable UUID tiendaId) {
        return ResponseEntity.ok(useCase.listar(tiendaId).stream().map(webMapper::toResponse).toList());
    }
}
