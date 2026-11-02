package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web;

import com.codefactory.supplychain.ventas.application.port.in.VentanaQuiebreUseCase;
import com.codefactory.supplychain.ventas.application.port.in.VentanaQuiebreUseCase.ConsultaFiltros;
import com.codefactory.supplychain.ventas.application.port.in.VentanaQuiebreUseCase.ReconciliarComando;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.ReconciliacionResponse;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.ReconciliarVentanasRequest;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.VentanaQuiebreResponse;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/** HU-46. Consultar exige ventas:consultar (D-45); reconciliar, inventario:administrar (D-49). */
@RestController
@RequestMapping("/api/v1/ventanas-quiebre")
@Tag(name = "Ventanas de quiebre", description = "Periodos en que una variante estuvo sin stock en el Almacén "
        + "de una tienda. Se abren cuando el saldo pasa a 0 o menos y se cierran al reponer o con una "
        + "venta forzada, sin intervención manual.")
public class VentanaQuiebreController {

    private final VentanaQuiebreUseCase useCase;

    public VentanaQuiebreController(VentanaQuiebreUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ventas:consultar')")
    @Operation(summary = "Consultar ventanas de quiebre",
            description = "Lista paginada (20 por defecto, más recientes primero) con filtros opcionales por tienda, "
                    + "variante, abierta y rango de fechas (ISO-8601). "
                    + "Solo ve registros de las tiendas en las que el usuario opera (o de todas con "
                    + "tiendas:operar-todas); un filtro por otra tienda devuelve una página vacía. "
                    + "Scope: ventas:consultar. HU-45, HU-46.")
    @ApiResponse(responseCode = "200", description = "Página de ventanas")
    public ResponseEntity<Page<VentanaQuiebreResponse>> consultar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @RequestParam(required = false) UUID tiendaId,
            @RequestParam(required = false) UUID varianteId,
            @RequestParam(required = false) Boolean abierta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @PageableDefault(size = 20, sort = "fechaInicio", direction = Sort.Direction.DESC)
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(useCase
                .consultar(usuario, new ConsultaFiltros(tiendaId, varianteId, abierta, desde, hasta), pageable)
                .map(VentanaQuiebreResponse::desde));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ventas:consultar')")
    @Operation(summary = "Obtener una ventana de quiebre", description = "Scope: ventas:consultar. HU-46.")
    @ApiResponse(responseCode = "200", description = "Ventana encontrada")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda en la que el usuario no opera")
    public ResponseEntity<VentanaQuiebreResponse> obtener(@AuthenticationPrincipal UsuarioActual usuario,
                                                          @PathVariable UUID id) {
        return ResponseEntity.ok(VentanaQuiebreResponse.desde(useCase.obtener(usuario, id)));
    }

    @PostMapping("/reconciliar")
    @PreAuthorize("hasAuthority('inventario:administrar')")
    @Operation(summary = "Reconciliar ventanas de quiebre",
            description = "Reconstruye desde el kardex los periodos sin stock de una tienda (opcionalmente de "
                    + "una variante y un rango de fechas): crea las ventanas que faltan, cierra las que el kardex "
                    + "muestra repuestas y corrige fechas. No borra nada: las ventanas sin respaldo en el kardex "
                    + "solo se cuentan. Idempotente. Scope: inventario:administrar. HU-46.")
    @ApiResponse(responseCode = "200", description = "Resumen de la reconciliación")
    @ApiResponse(responseCode = "400", description = "Rango de fechas inválido, tienda sin Almacén o cuerpo inválido")
    @ApiResponse(responseCode = "403", description = "Sin el scope o sin acceso a la tienda")
    public ResponseEntity<ReconciliacionResponse> reconciliar(@AuthenticationPrincipal UsuarioActual usuario,
                                                              @Valid @RequestBody ReconciliarVentanasRequest request) {
        return ResponseEntity.ok(ReconciliacionResponse.desde(useCase.reconciliar(new ReconciliarComando(
                usuario, request.tiendaId(), request.varianteId(), request.desde(), request.hasta()))));
    }
}
