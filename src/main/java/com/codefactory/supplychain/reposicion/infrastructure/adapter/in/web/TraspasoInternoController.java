package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.ConsultarTraspasosUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoManualUseCase;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.TraspasoInternoResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.TraspasoManualRequest;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.TraspasoInternoWebMapper;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Reposición interna", description = "Traspasos de la Bodega al Almacén de una tienda. Se "
        + "disparan solos cuando el Almacén queda por debajo del umbral mínimo; también se pueden registrar "
        + "a mano o forzar una evaluación. Scope: reposicion:administrar.")
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
    @Operation(summary = "Registrar un traspaso manual",
            description = "Mueve la cantidad de la Bodega al Almacén de la tienda en una sola operación; el "
                    + "operario se toma del token. Nunca deja la Bodega en negativo. HU-49.")
    @ApiResponse(responseCode = "201", description = "Traspaso registrado")
    @ApiResponse(responseCode = "403", description = "Sin el scope o sin acceso a la tienda")
    @ApiResponse(responseCode = "404", description = "Variante inexistente")
    @ApiResponse(responseCode = "409", description = "La Bodega no tiene stock suficiente")
    public ResponseEntity<TraspasoInternoResponse> registrarManual(
            @AuthenticationPrincipal UsuarioActual usuario,
            @PathVariable UUID tiendaId,
            @Valid @RequestBody TraspasoManualRequest request) {
        var traspaso = traspasoManual.registrar(usuario, tiendaId, request.varianteId(), request.cantidad());
        return ResponseEntity.status(HttpStatus.CREATED).body(webMapper.toResponse(traspaso));
    }

    /** Traspasos de una tienda, el más reciente primero. */
    @GetMapping("/api/v1/tiendas/{tiendaId}/traspasos-internos")
    @Operation(summary = "Listar los traspasos de una tienda",
            description = "Automáticos y manuales, el más reciente primero. Sin paginación. HU-49.")
    @ApiResponse(responseCode = "200", description = "Traspasos de la tienda")
    @ApiResponse(responseCode = "403", description = "Sin el scope o sin acceso a la tienda")
    public List<TraspasoInternoResponse> listarPorTienda(@AuthenticationPrincipal UsuarioActual usuario,
                                                         @PathVariable UUID tiendaId) {
        return consultarTraspasos.listarPorTienda(usuario, tiendaId).stream()
                .map(webMapper::toResponse)
                .toList();
    }

    @GetMapping("/api/v1/traspasos-internos/{id}")
    @Operation(summary = "Obtener un traspaso", description = "HU-49.")
    @ApiResponse(responseCode = "200", description = "Traspaso encontrado")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda en la que el usuario no opera")
    public TraspasoInternoResponse obtener(@AuthenticationPrincipal UsuarioActual usuario, @PathVariable UUID id) {
        return webMapper.toResponse(consultarTraspasos.obtener(usuario, id));
    }
}
