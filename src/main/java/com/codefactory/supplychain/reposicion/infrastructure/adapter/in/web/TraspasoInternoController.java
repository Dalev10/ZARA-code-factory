package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.ConsultarTraspasosUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoManualUseCase;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.TraspasoInternoResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.TraspasoManualRequest;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.TraspasoInternoWebMapper;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Registro manual y consulta de traspasos internos (HU-49). Solo sobre las tiendas
 * asignadas al usuario.
 *
 * Módulo: reposicion — Reposición interna.
 */
@RestController
@PreAuthorize("hasAuthority('reposicion:administrar')")
public class TraspasoInternoController {

    private final TraspasoManualUseCase traspasoManual;
    private final ConsultarTraspasosUseCase consultarTraspasos;
    private final TraspasoInternoWebMapper webMapper;

    public TraspasoInternoController(TraspasoManualUseCase traspasoManual,
                                     ConsultarTraspasosUseCase consultarTraspasos,
                                     TraspasoInternoWebMapper webMapper) {
        this.traspasoManual = traspasoManual;
        this.consultarTraspasos = consultarTraspasos;
        this.webMapper = webMapper;
    }

    /** El operario que lo registra se toma del token, nunca del cuerpo. */
    @PostMapping("/api/v1/tiendas/{tiendaId}/traspasos-internos")
    public ResponseEntity<TraspasoInternoResponse> registrarManual(
            @AuthenticationPrincipal UsuarioActual usuario,
            @PathVariable UUID tiendaId,
            @Valid @RequestBody TraspasoManualRequest request) {
        var traspaso = traspasoManual.registrar(usuario, tiendaId, request.varianteId(), request.cantidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(traspaso));
    }

    /** Traspasos de una tienda, el más reciente primero. */
    @GetMapping("/api/v1/tiendas/{tiendaId}/traspasos-internos")
    public List<TraspasoInternoResponse> listarPorTienda(@AuthenticationPrincipal UsuarioActual usuario,
                                                         @PathVariable UUID tiendaId) {
        return consultarTraspasos.listarPorTienda(usuario, tiendaId).stream()
                .map(webMapper::toResponse)
                .toList();
    }

    @GetMapping("/api/v1/traspasos-internos/{id}")
    public TraspasoInternoResponse obtener(@AuthenticationPrincipal UsuarioActual usuario, @PathVariable UUID id) {
        return webMapper.toResponse(consultarTraspasos.obtener(usuario, id));
    }
}
