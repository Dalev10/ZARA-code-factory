package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.EvaluacionMasivaUseCase;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.EvaluacionMasivaResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.TraspasoInternoWebMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * HU-50: evaluación masiva de reposición interna.
 * Protegida por {@code reposicion:administrar}.
 */
@RestController
@RequestMapping("/api/v1/reposicion-interna")
@PreAuthorize("hasAuthority('reposicion:administrar')")
public class EvaluacionMasivaController {

    private final EvaluacionMasivaUseCase evaluacionMasiva;
    private final TraspasoInternoWebMapper webMapper;

    public EvaluacionMasivaController(EvaluacionMasivaUseCase evaluacionMasiva,
                                      TraspasoInternoWebMapper webMapper) {
        this.evaluacionMasiva = evaluacionMasiva;
        this.webMapper = webMapper;
    }

    /**
     * Evalúa todas las tiendas activas y ejecuta los traspasos que procedan.
     * POST /api/v1/reposicion-interna/evaluar
     */
    @PostMapping("/evaluar")
    public ResponseEntity<EvaluacionMasivaResponse> evaluarTodas() {
        List<TraspasoInterno> traspasos = evaluacionMasiva.evaluarTodas();
        return ResponseEntity.ok(new EvaluacionMasivaResponse(
                traspasos.size(),
                traspasos.stream().map(webMapper::toResponse).toList()));
    }

    /**
     * Evalúa solo la tienda indicada. Útil para forzar la reposición de una tienda
     * concreta sin esperar al ciclo masivo.
     * POST /api/v1/reposicion-interna/tiendas/{tiendaId}/evaluar
     */
    @PostMapping("/tiendas/{tiendaId}/evaluar")
    public ResponseEntity<EvaluacionMasivaResponse> evaluarTienda(@PathVariable UUID tiendaId) {
        List<TraspasoInterno> traspasos = evaluacionMasiva.evaluarTienda(tiendaId);
        return ResponseEntity.ok(new EvaluacionMasivaResponse(
                traspasos.size(),
                traspasos.stream().map(webMapper::toResponse).toList()));
    }
}
