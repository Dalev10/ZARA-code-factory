package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web;

import com.codefactory.supplychain.ventas.application.port.in.SolicitudNoSatisfechaUseCase;
import com.codefactory.supplychain.ventas.application.port.in.SolicitudNoSatisfechaUseCase.ConsultaFiltros;
import com.codefactory.supplychain.ventas.application.port.in.SolicitudNoSatisfechaUseCase.RegistrarComando;
import com.codefactory.supplychain.ventas.application.port.in.SolicitudNoSatisfechaUseCase.ResultadoRegistro;
import com.codefactory.supplychain.ventas.domain.model.EstadoSolicitud;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.RegistrarSolicitudRequest;
import com.codefactory.supplychain.ventas.infrastructure.adapter.in.web.dto.SolicitudNoSatisfechaResponse;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

/** HU-34 y HU-35. Scopes: ventas:registrar, ventas:consultar, ventas:anular. */
@RestController
@RequestMapping("/api/v1/solicitudes-no-satisfechas")
public class SolicitudNoSatisfechaController {

    private final SolicitudNoSatisfechaUseCase useCase;

    public SolicitudNoSatisfechaController(SolicitudNoSatisfechaUseCase useCase) {
        this.useCase = useCase;
    }

    /** 201 al crear; 200 con la solicitud original si es una repetición con la misma clave. */
    @PostMapping
    @PreAuthorize("hasAuthority('ventas:registrar')")
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
    public ResponseEntity<Page<SolicitudNoSatisfechaResponse>> consultar(
            @AuthenticationPrincipal UsuarioActual usuario,
            @RequestParam(required = false) UUID tiendaId,
            @RequestParam(required = false) UUID varianteId,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @PageableDefault(size = 20, sort = "registradaEn", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(useCase
                .consultar(usuario, new ConsultaFiltros(tiendaId, varianteId, estado, desde, hasta), pageable)
                .map(SolicitudNoSatisfechaResponse::desde));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ventas:consultar')")
    public ResponseEntity<SolicitudNoSatisfechaResponse> obtener(@AuthenticationPrincipal UsuarioActual usuario,
                                                                 @PathVariable UUID id) {
        return ResponseEntity.ok(SolicitudNoSatisfechaResponse.desde(useCase.obtener(usuario, id)));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('ventas:anular')")
    public ResponseEntity<SolicitudNoSatisfechaResponse> anular(@AuthenticationPrincipal UsuarioActual usuario,
                                                                @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(SolicitudNoSatisfechaResponse.desde(useCase.anular(usuario, id)));
    }
}
