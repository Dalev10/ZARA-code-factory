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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Ventas", description = "Registro, consulta y anulación de ventas. Cada línea descuenta o "
        + "repone stock del Almacén de la tienda y deja su movimiento en el kardex.")
public class VentaController {

    private static final String EJEMPLO_REGISTRAR = """
            {"transaccionId": "9b2f6c1d-0e4a-4f7b-8c3d-5a1e2b3c4d5e",
             "tiendaId": "1c7e9a2b-3d4f-4a5b-9c6d-7e8f9a0b1c2d", "canal": "POS",
             "items": [{"sku": "CAM-OXF-AZ-M", "cantidad": 2, "precioUnitario": 89900.00},
                       {"sku": "PAN-CHI-BE-32", "cantidad": 1, "precioUnitario": 129900.00}]}
            """;
    private static final String EJEMPLO_VENTA = """
            {"transaccionId": "9b2f6c1d-0e4a-4f7b-8c3d-5a1e2b3c4d5e",
             "tiendaId": "1c7e9a2b-3d4f-4a5b-9c6d-7e8f9a0b1c2d",
             "vendedorId": "3f8c2a1e-5b7d-4c9a-8e21-6d4f0b9a7c13", "fecha": "2026-11-01T15:42:10Z",
             "total": 309700.00,
             "lineas": [{"id": "5d1a3b7c-8e9f-4a0b-b1c2-d3e4f5a6b7c8",
                         "varianteId": "6e2b4c8d-9f0a-4b1c-a2d3-e4f5a6b7c8d9", "cantidad": 2,
                         "precioUnitario": 89900.00, "canal": "POS", "forzada": false,
                         "estado": "REGISTRADA", "anuladaPor": null, "anuladaEn": null,
                         "motivoAnulacion": null},
                        {"id": "7f3c5d9e-0a1b-4c2d-b3e4-f5a6b7c8d9e0",
                         "varianteId": "8a4d6e0f-1b2c-4d3e-a4f5-a6b7c8d9e0f1", "cantidad": 1,
                         "precioUnitario": 129900.00, "canal": "POS", "forzada": false,
                         "estado": "REGISTRADA", "anuladaPor": null, "anuladaEn": null,
                         "motivoAnulacion": null}]}
            """;


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
    @Operation(summary = "Registrar una venta",
            description = "Registra una transacción de hasta 200 ítems (SKU, cantidad y precio unitario) y descuenta "
                    + "el stock del Almacén de la tienda en una sola operación: o se registran todas las líneas "
                    + "o ninguna. Sin stock suficiente responde 409, salvo forzarSinStock=true, que exige además "
                    + "el scope ventas:forzar-sin-stock. Idempotente por transaccionId: repetir la misma venta "
                    + "devuelve 200 con la original; reusar el id con otro contenido responde 409. "
                    + "Scope: ventas:registrar. HU-40.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(value = EJEMPLO_REGISTRAR)))
    @ApiResponse(responseCode = "201", description = "Venta registrada",
            content = @Content(schema = @Schema(implementation = VentaResponse.class),
                    examples = @ExampleObject(value = EJEMPLO_VENTA)))
    @ApiResponse(responseCode = "200", description = "Reintento: la venta ya existía con el mismo contenido",
            content = @Content(schema = @Schema(implementation = VentaResponse.class)))
    @ApiResponse(responseCode = "400", description = "Ítems inválidos o repetidos, canal fuera de rango, tienda "
            + "inactiva o cuerpo inválido")
    @ApiResponse(responseCode = "403", description = "Sin el scope, sin acceso a la tienda o forzarSinStock sin "
            + "ventas:forzar-sin-stock")
    @ApiResponse(responseCode = "404", description = "Tienda sin Almacén o SKU inexistente")
    @ApiResponse(responseCode = "409", description = "Stock insuficiente, o transaccionId ya usado con otro "
            + "contenido")
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
    @Operation(summary = "Consultar ventas",
            description = "Lista paginada (20 por defecto, más recientes primero), una línea por elemento. Filtros "
                    + "opcionales por tienda, variante, transacción, estado y rango de fechas (ISO-8601). "
                    + "Solo ve registros de las tiendas en las que el usuario opera (o de todas con "
                    + "tiendas:operar-todas); un filtro por otra tienda devuelve una página vacía. "
                    + "Scope: ventas:consultar. HU-42.")
    @ApiResponse(responseCode = "200", description = "Página de ventas")
    @ApiResponse(responseCode = "400", description = "desde posterior a hasta, o parámetro con formato inválido")
    public ResponseEntity<Page<VentaResponse>> consultar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @RequestParam(required = false) UUID tiendaId,
            @RequestParam(required = false) UUID varianteId,
            @RequestParam(required = false) UUID transaccionId,
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @PageableDefault(size = 20, sort = "timestampVenta", direction = Sort.Direction.DESC)
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(consultarVentasUseCase
                .consultar(usuario, new Filtros(tiendaId, varianteId, transaccionId, estado, desde, hasta), pageable)
                .map(venta -> VentaResponse.from(List.of(venta))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ventas:consultar')")
    @Operation(summary = "Obtener una venta",
            description = "Una línea de venta por su id. Scope: ventas:consultar. HU-42.")
    @ApiResponse(responseCode = "200", description = "Venta encontrada")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda en la que el usuario no opera")
    public ResponseEntity<VentaResponse> obtener(@AuthenticationPrincipal UsuarioActual usuario,
                                                 @PathVariable UUID id) {
        Venta venta = consultarVentasUseCase.obtener(usuario, id);
        return ResponseEntity.ok(VentaResponse.from(List.of(venta)));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('ventas:anular')")
    @Operation(summary = "Anular una línea de venta",
            description = "Marca la línea como ANULADA con el motivo y repone su cantidad al Almacén. Idempotente: "
                    + "si ya estaba anulada, devuelve 200 sin reponer de nuevo. Scope: ventas:anular. HU-41.")
    @ApiResponse(responseCode = "200", description = "Línea anulada")
    @ApiResponse(responseCode = "400", description = "Motivo vacío o de más de 500 caracteres")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda en la que el usuario no opera")
    public ResponseEntity<VentaResponse> anular(@AuthenticationPrincipal UsuarioActual usuario,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody AnularVentaRequest request) {
        return ResponseEntity.ok(VentaResponse.from(List.of(
                anularVentaUseCase.anular(usuario, id, request.motivo()))));
    }

    @PostMapping("/transacciones/{transaccionId}/anular")
    @PreAuthorize("hasAuthority('ventas:anular')")
    @Operation(summary = "Anular una transacción completa",
            description = "Anula y repone todas las líneas aún vigentes de la transacción, en una sola operación. "
                    + "Idempotente. Scope: ventas:anular. HU-41.")
    @ApiResponse(responseCode = "200", description = "Transacción anulada")
    @ApiResponse(responseCode = "400", description = "Motivo vacío o de más de 500 caracteres")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda en la que el usuario no opera")
    public ResponseEntity<VentaResponse> anularTransaccion(@AuthenticationPrincipal UsuarioActual usuario,
                                                           @PathVariable UUID transaccionId,
                                                           @Valid @RequestBody AnularVentaRequest request) {
        return ResponseEntity.ok(VentaResponse.from(
                anularVentaUseCase.anularTransaccion(usuario, transaccionId, request.motivo())));
    }
}
