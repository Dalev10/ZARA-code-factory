package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web;

import com.codefactory.supplychain.ventas.application.port.in.VentanaQuiebreUseCase;
import com.codefactory.supplychain.ventas.application.port.in.VentanaQuiebreUseCase.ConsultaFiltros;
import com.codefactory.supplychain.ventas.application.port.in.VentanaQuiebreUseCase.ReconciliarComando;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.ReconciliacionResponse;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.ReconciliarVentanasRequest;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.VentanaQuiebreResponse;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import jakarta.validation.Valid;
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

/** HU-37. Todo el controlador exige el scope inventario:administrar (según la especificación). */
@RestController
@RequestMapping("/api/v1/ventanas-quiebre")
@PreAuthorize("hasAuthority('inventario:administrar')")
public class VentanaQuiebreController {

    private final VentanaQuiebreUseCase useCase;

    public VentanaQuiebreController(VentanaQuiebreUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public ResponseEntity<Page<VentanaQuiebreResponse>> consultar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @RequestParam(required = false) UUID tiendaId,
            @RequestParam(required = false) UUID varianteId,
            @RequestParam(required = false) Boolean abierta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @PageableDefault(size = 20, sort = "fechaInicio", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(useCase
                .consultar(usuario, new ConsultaFiltros(tiendaId, varianteId, abierta, desde, hasta), pageable)
                .map(VentanaQuiebreResponse::desde));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentanaQuiebreResponse> obtener(@AuthenticationPrincipal UsuarioActual usuario,
                                                          @PathVariable UUID id) {
        return ResponseEntity.ok(VentanaQuiebreResponse.desde(useCase.obtener(usuario, id)));
    }

    @PostMapping("/reconciliar")
    public ResponseEntity<ReconciliacionResponse> reconciliar(@AuthenticationPrincipal UsuarioActual usuario,
                                                              @Valid @RequestBody ReconciliarVentanasRequest request) {
        return ResponseEntity.ok(ReconciliacionResponse.desde(useCase.reconciliar(new ReconciliarComando(
                usuario, request.tiendaId(), request.varianteId(), request.desde(), request.hasta()))));
    }
}
