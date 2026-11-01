package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web;

import com.codefactory.supplychain.ventas.application.port.in.SolicitudNoSatisfechaUseCase;
import com.codefactory.supplychain.ventas.application.port.in.SolicitudNoSatisfechaUseCase.ConsultaFiltros;
import com.codefactory.supplychain.ventas.application.port.in.SolicitudNoSatisfechaUseCase.RegistrarComando;
import com.codefactory.supplychain.ventas.application.port.in.SolicitudNoSatisfechaUseCase.ResultadoRegistro;
import com.codefactory.supplychain.ventas.domain.model.EstadoSolicitud;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.AnularSolicitudRequest;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.RegistrarSolicitudRequest;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.SolicitudNoSatisfechaResponse;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
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

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/** HU-43 y HU-44. Scopes: ventas:registrar, ventas:consultar, ventas:anular. */
@RestController
@RequestMapping("/api/v1/solicitudes-no-satisfechas")
@Tag(name = "Demanda no satisfecha", description = "Registro de lo que un cliente pidió y la tienda no pudo "
        + "vender, para medir la demanda perdida.")
public class SolicitudNoSatisfechaController {

    private static final String EJEMPLO_REGISTRAR = """
            {"tiendaId": "1c7e9a2b-3d4f-4a5b-9c6d-7e8f9a0b1c2d",
             "varianteId": "6e2b4c8d-9f0a-4b1c-a2d3-e4f5a6b7c8d9", "cantidad": 1,
             "claveIdempotencia": "POS-07-20261101-154210"}
            """;
    private static final String EJEMPLO_SOLICITUD = """
            {"id": "2a9c4e6f-7b8d-4e0f-a1b2-c3d4e5f6a7b8", "tiendaId": "1c7e9a2b-3d4f-4a5b-9c6d-7e8f9a0b1c2d",
             "varianteId": "6e2b4c8d-9f0a-4b1c-a2d3-e4f5a6b7c8d9", "cantidad": 1, "stockSistema": 0,
             "registradaEn": "2026-11-01T15:42:10Z", "registradoPor": "3f8c2a1e-5b7d-4c9a-8e21-6d4f0b9a7c13",
             "claveIdempotencia": "POS-07-20261101-154210", "estado": "ACTIVA", "anuladaPor": null,
             "anuladaEn": null, "motivoAnulacion": null}
            """;


    private final SolicitudNoSatisfechaUseCase useCase;

    public SolicitudNoSatisfechaController(SolicitudNoSatisfechaUseCase useCase) {
        this.useCase = useCase;
    }

    /** 201 al crear; 200 con la solicitud original si es una repetición con la misma clave. */
    @PostMapping
    @PreAuthorize("hasAuthority('ventas:registrar')")
    @Operation(summary = "Registrar una solicitud no satisfecha",
            description = "Guarda la variante y cantidad pedidas junto con el stock que el sistema mostraba en el "
                    + "Almacén en ese momento (stockSistema). Idempotente por claveIdempotencia: repetir la misma "
                    + "solicitud devuelve 200 con la original; reusar la clave con otros datos responde 409. "
                    + "Scope: ventas:registrar. HU-43.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(value = EJEMPLO_REGISTRAR)))
    @ApiResponse(responseCode = "201", description = "Solicitud registrada",
            content = @Content(schema = @Schema(implementation = SolicitudNoSatisfechaResponse.class),
                    examples = @ExampleObject(value = EJEMPLO_SOLICITUD)))
    @ApiResponse(responseCode = "200", description = "Repetición: ya existía con la misma clave y datos",
            content = @Content(schema = @Schema(implementation = SolicitudNoSatisfechaResponse.class)))
    @ApiResponse(responseCode = "400", description = "Cantidad no positiva, clave vacía, tienda sin Almacén o "
            + "cuerpo inválido")
    @ApiResponse(responseCode = "403", description = "Sin el scope o sin acceso a la tienda")
    @ApiResponse(responseCode = "404", description = "Variante inexistente")
    @ApiResponse(responseCode = "409", description = "La clave ya se usó con datos distintos")
    public ResponseEntity<SolicitudNoSatisfechaResponse> registrar(@AuthenticationPrincipal UsuarioActual usuario,
                                                                   @Valid @RequestBody RegistrarSolicitudRequest request) {
        ResultadoRegistro resultado = useCase.registrar(new RegistrarComando(usuario, request.tiendaId(),
                request.varianteId(), request.cantidad(), request.claveIdempotencia()));
        var cuerpo = SolicitudNoSatisfechaResponse.desde(resultado.solicitud());
        if (resultado.creada()) {
            return ResponseEntity.created(URI.create("/api/v1/solicitudes-no-satisfechas/" + cuerpo.id())).body(cuerpo);
        }
        return ResponseEntity.ok(cuerpo);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ventas:consultar')")
    @Operation(summary = "Consultar solicitudes no satisfechas",
            description = "Lista paginada (20 por defecto, más recientes primero) con filtros opcionales por tienda, "
                    + "variante, estado y rango de fechas (ISO-8601). "
                    + "Solo ve registros de las tiendas en las que el usuario opera (o de todas con "
                    + "tiendas:operar-todas); un filtro por otra tienda devuelve una página vacía. "
                    + "Scope: ventas:consultar. HU-44.")
    @ApiResponse(responseCode = "200", description = "Página de solicitudes")
    public ResponseEntity<Page<SolicitudNoSatisfechaResponse>> consultar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @RequestParam(required = false) UUID tiendaId,
            @RequestParam(required = false) UUID varianteId,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @PageableDefault(size = 20, sort = "registradaEn", direction = Sort.Direction.DESC)
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(useCase
                .consultar(usuario, new ConsultaFiltros(tiendaId, varianteId, estado, desde, hasta), pageable)
                .map(SolicitudNoSatisfechaResponse::desde));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ventas:consultar')")
    @Operation(summary = "Obtener una solicitud no satisfecha", description = "Scope: ventas:consultar. HU-44.")
    @ApiResponse(responseCode = "200", description = "Solicitud encontrada")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda en la que el usuario no opera")
    public ResponseEntity<SolicitudNoSatisfechaResponse> obtener(@AuthenticationPrincipal UsuarioActual usuario,
                                                                 @PathVariable UUID id) {
        return ResponseEntity.ok(SolicitudNoSatisfechaResponse.desde(useCase.obtener(usuario, id)));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('ventas:anular')")
    @Operation(summary = "Anular una solicitud no satisfecha",
            description = "Requiere motivo. Idempotente: si ya estaba anulada, devuelve 200 sin cambios. "
                    + "Scope: ventas:anular. HU-44.")
    @ApiResponse(responseCode = "200", description = "Solicitud anulada")
    @ApiResponse(responseCode = "404", description = "No existe o es de una tienda en la que el usuario no opera")
    public ResponseEntity<SolicitudNoSatisfechaResponse> anular(@AuthenticationPrincipal UsuarioActual usuario,
                                                                @PathVariable UUID id,
                                                                @Valid @RequestBody AnularSolicitudRequest request) {
        return ResponseEntity.ok(SolicitudNoSatisfechaResponse.desde(useCase.anular(usuario, id, request.motivo())));
    }
}
