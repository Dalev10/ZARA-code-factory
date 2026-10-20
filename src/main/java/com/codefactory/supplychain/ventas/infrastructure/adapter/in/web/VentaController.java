package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web;

import com.codefactory.supplychain.ventas.application.port.in.AnularVentaUseCase;
import com.codefactory.supplychain.ventas.application.port.in.ConsultarVentasUseCase;
import com.codefactory.supplychain.ventas.application.port.in.ConsultarVentasUseCase.Filtros;
import com.codefactory.supplychain.ventas.application.port.in.RegistrarVentaUseCase;
import com.codefactory.supplychain.ventas.application.port.in.RegistrarVentaUseCase.Comando;
import com.codefactory.supplychain.ventas.application.port.in.RegistrarVentaUseCase.Item;
import com.codefactory.supplychain.ventas.application.port.in.RegistrarVentaUseCase.ResultadoVenta;
import com.codefactory.supplychain.ventas.domain.model.EstadoVenta;
import com.codefactory.supplychain.ventas.domain.model.Venta;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.AnularVentaRequest;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.RegistrarVentaRequest;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.VentaResponse;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Módulo: ventas — Registro de venta, Solicitud_No_Satisfecha y Ventana_Quiebre.
 */
@RestController
@RequestMapping("/api/v1/ventas")
public class VentaController {

    private final RegistrarVentaUseCase registrarVentaUseCase;
    private final ConsultarVentasUseCase consultarVentasUseCase;
    private final AnularVentaUseCase anularVentaUseCase;

    public VentaController(RegistrarVentaUseCase registrarVentaUseCase,
                           ConsultarVentasUseCase consultarVentasUseCase,
                           AnularVentaUseCase anularVentaUseCase) {
        this.registrarVentaUseCase = registrarVentaUseCase;
        this.consultarVentasUseCase = consultarVentasUseCase;
        this.anularVentaUseCase = anularVentaUseCase;
    }

    /** 201 si se creó; 200 con la venta original si es un reintento con la misma transacción (D-69). */
    @PostMapping
    @PreAuthorize("hasAuthority('ventas:registrar')")
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody RegistrarVentaRequest request,
                                                   @AuthenticationPrincipal UsuarioActual actor) {
        ResultadoVenta resultado = registrarVentaUseCase.registrar(new Comando(actor, request.transaccionId(),
                request.tiendaId(), request.canal(), request.forzarSinStock(),
                request.items().stream().map(i -> new Item(i.sku(), i.cantidad(), i.precioUnitario())).toList()));
        return ResponseEntity.status(resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(VentaResponse.from(resultado.lineas()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ventas:consultar')")
    public ResponseEntity<Page<VentaResponse>> consultar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @RequestParam(required = false) UUID tiendaId,
            @RequestParam(required = false) UUID varianteId,
            @RequestParam(required = false) UUID transaccionId,
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @PageableDefault(size = 20, sort = "timestampVenta", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(consultarVentasUseCase
                .consultar(usuario, new Filtros(tiendaId, varianteId, transaccionId, estado, desde, hasta), pageable)
                .map(venta -> VentaResponse.from(List.of(venta))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ventas:consultar')")
    public ResponseEntity<VentaResponse> obtener(@AuthenticationPrincipal UsuarioActual usuario,
                                                 @PathVariable UUID id) {
        Venta venta = consultarVentasUseCase.obtener(usuario, id);
        return ResponseEntity.ok(VentaResponse.from(List.of(venta)));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('ventas:anular')")
    public ResponseEntity<VentaResponse> anular(@AuthenticationPrincipal UsuarioActual usuario,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody AnularVentaRequest request) {
        return ResponseEntity.ok(VentaResponse.from(List.of(
                anularVentaUseCase.anular(usuario, id, request.motivo()))));
    }

    @PostMapping("/transacciones/{transaccionId}/anular")
    @PreAuthorize("hasAuthority('ventas:anular')")
    public ResponseEntity<VentaResponse> anularTransaccion(@AuthenticationPrincipal UsuarioActual usuario,
                                                           @PathVariable UUID transaccionId,
                                                           @Valid @RequestBody AnularVentaRequest request) {
        return ResponseEntity.ok(VentaResponse.from(
                anularVentaUseCase.anularTransaccion(usuario, transaccionId, request.motivo())));
    }
}
