package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web;

import com.codefactory.supplychain.logistica.application.port.in.ConsultarCiclosDespachoUseCase;
import com.codefactory.supplychain.logistica.application.port.in.DespacharOrdenUseCase;
import com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto.CicloDespachoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Despacho de órdenes desde el CD (HU-46). Despachar exige {@code despacho:operar}; consultar
 * los ciclos también lo permite {@code transporte:operar}, porque el coordinador arma los viajes
 * con ellos (HU-47).
 *
 * Módulo: logistica.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Despacho", description = "Salida de mercancía del CD para una orden de reabastecimiento. Cada despacho "
        + "crea un ciclo con lo que realmente había a la mano. Scopes: despacho:operar (despachar y consultar) y "
        + "transporte:operar (consultar).")
public class DespachoController {

    private final DespacharOrdenUseCase despacharOrden;
    private final ConsultarCiclosDespachoUseCase consultarCiclos;

    @PostMapping("/ordenes-reabastecimiento/{ordenId}/despachos")
    @PreAuthorize("hasAuthority('despacho:operar')")
    @Operation(summary = "Despachar una orden",
            description = "La orden debe estar APROBADA o REABIERTA. Sale del CD lo menor entre el pendiente de la "
                    + "orden y el a_la_mano real; el ciclo queda COMPLETO si salió todo o PARCIAL si no, con su "
                    + "fill rate. Descuenta el CD (kardex DESPACHO), libera la reserva hasta lo despachado y deja "
                    + "la orden DESPACHADA. Sin cuerpo. HU-46.")
    @ApiResponse(responseCode = "201", description = "Ciclo de despacho creado")
    @ApiResponse(responseCode = "404", description = "La orden no existe")
    @ApiResponse(responseCode = "409", description = "La orden no está APROBADA ni REABIERTA, otro despacho de la "
            + "misma orden ganó, o el CD no tiene nada a la mano; no se crea ciclo ni cambia la orden")
    public ResponseEntity<CicloDespachoResponse> despachar(@PathVariable UUID ordenId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CicloDespachoResponse.desde(despacharOrden.despachar(ordenId)));
    }

    @GetMapping("/ciclos-despacho")
    @PreAuthorize("hasAnyAuthority('despacho:operar', 'transporte:operar')")
    @Operation(summary = "Listar ciclos de despacho",
            description = "Del más reciente al más antiguo; el parámetro sort no aplica. Con sinViaje=true, solo "
                    + "los que todavía no están en un viaje. HU-46.")
    @ApiResponse(responseCode = "200", description = "Página de ciclos")
    public Page<CicloDespachoResponse> listar(
            @Parameter(description = "Solo los ciclos de esta orden") @RequestParam(required = false) UUID ordenId,
            @Parameter(description = "Solo los que aún no están en un viaje")
            @RequestParam(defaultValue = "false") boolean sinViaje,
            @ParameterObject Pageable pageable) {
        return consultarCiclos.listar(ordenId, sinViaje, pageable).map(CicloDespachoResponse::desde);
    }

    @GetMapping("/ciclos-despacho/{id}")
    @PreAuthorize("hasAnyAuthority('despacho:operar', 'transporte:operar')")
    @Operation(summary = "Consultar un ciclo de despacho", description = "HU-46.")
    @ApiResponse(responseCode = "200", description = "Ciclo de despacho")
    @ApiResponse(responseCode = "404", description = "El ciclo no existe")
    public CicloDespachoResponse obtener(@PathVariable UUID id) {
        return CicloDespachoResponse.desde(consultarCiclos.obtener(id));
    }
}
