package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web;

import com.codefactory.supplychain.reposicion.application.port.in.EvaluacionMasivaUseCase;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.dto.EvaluacionMasivaResponse;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.mapper.TraspasoInternoWebMapper;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@Tag(name = "Reposición interna", description = "Traspasos de la Bodega al Almacén de una tienda. Se "
        + "disparan solos cuando el Almacén queda por debajo del umbral mínimo; también se pueden registrar "
        + "a mano o forzar una evaluación. Scope: reposicion:administrar.")
public class EvaluacionMasivaController {

    private final EvaluacionMasivaUseCase evaluacionMasiva;
    private final TraspasoInternoWebMapper webMapper;

    public EvaluacionMasivaController(EvaluacionMasivaUseCase evaluacionMasiva,
                                      TraspasoInternoWebMapper webMapper) {
        this.evaluacionMasiva = evaluacionMasiva;
        this.webMapper = webMapper;
    }

    /**
     * Evalúa las tiendas activas del usuario y ejecuta los traspasos que procedan.
     * POST /api/v1/reposicion-interna/evaluar
     */
    @PostMapping("/evaluar")
    @Operation(summary = "Evaluar la reposición de todas las tiendas",
            description = "Recorre las tiendas activas en las que el usuario opera (todas, con "
                    + "tiendas:operar-todas) y ejecuta los traspasos que procedan. Por cada variante "
                    + "con inventario, si el Almacén está por debajo del mínimo de su umbral (el de la variante "
                    + "o, si no tiene, el de su categoría), traslada MIN(objetivo − MAX(Almacén, 0), Bodega). Una "
                    + "variante que falla se registra en el log y no detiene al resto. HU-50.")
    @ApiResponse(responseCode = "200", description = "Traspasos ejecutados")
    public ResponseEntity<EvaluacionMasivaResponse> evaluarTodas(@AuthenticationPrincipal UsuarioActual usuario) {
        List<TraspasoInterno> traspasos = evaluacionMasiva.evaluarTodas(usuario);
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
    @Operation(summary = "Evaluar la reposición de una tienda",
            description = "La misma evaluación, solo para la tienda indicada. Una tienda sin inventario "
                    + "devuelve 0 traspasos. HU-50.")
    @ApiResponse(responseCode = "200", description = "Traspasos ejecutados")
    @ApiResponse(responseCode = "403", description = "Sin el scope o sin acceso a la tienda")
    public ResponseEntity<EvaluacionMasivaResponse> evaluarTienda(@AuthenticationPrincipal UsuarioActual usuario,
                                                                  @PathVariable UUID tiendaId) {
        List<TraspasoInterno> traspasos = evaluacionMasiva.evaluarTienda(usuario, tiendaId);
        return ResponseEntity.ok(new EvaluacionMasivaResponse(
                traspasos.size(),
                traspasos.stream().map(webMapper::toResponse).toList()));
    }
}
