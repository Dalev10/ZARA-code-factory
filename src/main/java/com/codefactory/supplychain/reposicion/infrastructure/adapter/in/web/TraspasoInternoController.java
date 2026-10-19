package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.TraspasoManualUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.TraspasoInternoRepository;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.TraspasoInternoResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.TraspasoManualRequest;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.TraspasoInternoWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Consulta y registro manual de traspasos internos (HU-39 GET + HU-40 POST).
 * Rutas del mapa:
 * <ul>
 *   <li>{@code POST /api/v1/tiendas/{tiendaId}/traspasos-internos} — traspaso manual (HU-40)</li>
 *   <li>{@code GET  /api/v1/tiendas/{tiendaId}/traspasos-internos} — historial (HU-39)</li>
 *   <li>{@code GET  /api/v1/traspasos-internos/{id}} — detalle (HU-39)</li>
 * </ul>
 *
 * Módulo: reposicion — Reposición interna.
 */
@RestController
@PreAuthorize("hasAuthority('reposicion:administrar')")
public class TraspasoInternoController {

    private final TraspasoManualUseCase traspasoManual;
    private final TraspasoInternoRepository repository;
    private final TraspasoInternoWebMapper webMapper;

    public TraspasoInternoController(TraspasoManualUseCase traspasoManual,
                                      TraspasoInternoRepository repository,
                                      TraspasoInternoWebMapper webMapper) {
        this.traspasoManual = traspasoManual;
        this.repository = repository;
        this.webMapper = webMapper;
    }

    /**
     * Registra un traspaso manual. El operario que lo ejecuta se toma del token JWT.
     * {@code POST /api/v1/tiendas/{tiendaId}/traspasos-internos}
     * <p>
     * {@code @AuthenticationPrincipal UsuarioActual principal} y usar
     * {@code principal.id()} como {@code registradoPor}.
     */
    @PostMapping("/api/v1/tiendas/{tiendaId}/traspasos-internos")
    public ResponseEntity<TraspasoInternoResponse> registrarManual(
            @PathVariable UUID tiendaId,
            @Valid @RequestBody TraspasoManualRequest request) {

        // TODO: extraer del token → UUID registradoPor = principal.id();
        UUID registradoPor = extraerUsuarioDummy();

        var traspaso = traspasoManual.registrar(
                tiendaId, request.varianteId(), request.cantidad(), registradoPor);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(webMapper.toResponse(traspaso));
    }

    /**
     * Lista los traspasos de una tienda, más reciente primero.
     * {@code GET /api/v1/tiendas/{tiendaId}/traspasos-internos}
     */
    @GetMapping("/api/v1/tiendas/{tiendaId}/traspasos-internos")
    public List<TraspasoInternoResponse> listarPorTienda(@PathVariable UUID tiendaId) {
        return repository.listarPorTienda(tiendaId)
                .stream()
                .map(webMapper::toResponse)
                .toList();
    }

    /**
     * Detalle de un traspaso.
     * {@code GET /api/v1/traspasos-internos/{id}}
     */
    @GetMapping("/api/v1/traspasos-internos/{id}")
    public ResponseEntity<TraspasoInternoResponse> obtener(@PathVariable UUID id) {
        return repository.buscarPorId(id)
                .map(webMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Stub temporal hasta que se exponga UsuarioActual en shared.
     */
    private UUID extraerUsuarioDummy() {
        throw new UnsupportedOperationException(
                "UsuarioActual aún no disponible — pendiente");
    }
}
