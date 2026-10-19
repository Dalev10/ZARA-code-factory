package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.PrioridadTiendaCdUseCase;
import com.codefactory.supplychain.inventario.domain.model.PrioridadTiendaCd;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.PrioridadTiendaCdRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.PrioridadTiendaCdResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.mapper.PrioridadTiendaCdWebMapper;
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
public class PrioridadTiendaCdController {

    private final PrioridadTiendaCdUseCase useCase;
    private final PrioridadTiendaCdWebMapper webMapper;

    public PrioridadTiendaCdController(PrioridadTiendaCdUseCase useCase,
                                       PrioridadTiendaCdWebMapper webMapper) {
        this.useCase = useCase;
        this.webMapper = webMapper;
    }

    @PutMapping
    public ResponseEntity<List<PrioridadTiendaCdResponse>> reemplazar(
            @PathVariable UUID tiendaId,
            @Valid @RequestBody PrioridadTiendaCdRequest request) {

        List<PrioridadTiendaCd> prioridades = useCase.reemplazar(tiendaId, request.cdIds());
        return ResponseEntity.ok(prioridades.stream().map(webMapper::toResponse).toList());
    }

    @GetMapping
    public ResponseEntity<List<PrioridadTiendaCdResponse>> listar(@PathVariable UUID tiendaId) {
        return ResponseEntity.ok(useCase.listar(tiendaId).stream().map(webMapper::toResponse).toList());
    }
}
