package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web;

import com.codefactory.supplychain.logistica.application.port.in.ArmarViajeUseCase;
import com.codefactory.supplychain.logistica.application.port.in.ArmarViajeUseCase.Comando;
import com.codefactory.supplychain.logistica.application.port.in.ArmarViajeUseCase.ParadaPedida;
import com.codefactory.supplychain.logistica.application.port.in.ConsultarViajesUseCase;
import com.codefactory.supplychain.logistica.application.port.in.RegistrarTrayectoViajeUseCase;
import com.codefactory.supplychain.logistica.domain.model.EstadoViaje;
import com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto.ArmarViajeRequest;
import com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto.RegistrarLlegadaRequest;
import com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto.RegistrarSalidaRequest;
import com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto.ViajeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Viajes del CD a las tiendas: se arman (HU-47), salen y llegan a cada parada (HU-48). Scope
 * {@code transporte:operar}.
 *
 * Módulo: logistica.
 */
@RestController
@RequestMapping("/api/v1/viajes")
@PreAuthorize("hasAuthority('transporte:operar')")
@RequiredArgsConstructor
@Tag(name = "Viajes", description = "Viajes que salen de un CD con varias paradas, una por ciclo de despacho: se "
        + "arman, salen y llegan a cada tienda. Scope: transporte:operar.")
public class ViajeController {

    private static final String EJEMPLO_ARMAR = """
            {"cdId": "0b6c3a52-7a8e-4f0e-9f3c-2d1e4b5a6c7d", "vehiculo": "Camión ABC-123",
             "paradas": [{"cicloDespachoId": "9d8c7b6a-5f4e-4d3c-8b2a-1f0e9d8c7b6a"},
                         {"cicloDespachoId": "1a2b3c4d-5e6f-4a1b-8c2d-3e4f5a6b7c8d", "ordenParada": 2}]}""";

    private final ArmarViajeUseCase armarViaje;
    private final ConsultarViajesUseCase consultarViajes;
    private final RegistrarTrayectoViajeUseCase trayecto;

    @PostMapping
    @Operation(summary = "Armar un viaje",
            description = "Crea el viaje en ARMADO con una parada PENDIENTE por ciclo de despacho. La tienda de cada "
                    + "parada es la de la orden del ciclo. Todos los ciclos deben salir del CD del viaje y no estar "
                    + "en otro viaje. ordenParada es opcional: por defecto, la posición en la lista. HU-47.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            examples = @ExampleObject(value = EJEMPLO_ARMAR)))
    @ApiResponse(responseCode = "201", description = "Viaje armado")
    @ApiResponse(responseCode = "400", description = "Sin paradas, sin vehículo, ciclo u orden de parada repetido")
    @ApiResponse(responseCode = "404", description = "El CD o algún ciclo de despacho no existe")
    @ApiResponse(responseCode = "409", description = "Algún ciclo ya está en otro viaje")
    @ApiResponse(responseCode = "422", description = "Algún ciclo es de una orden de otro CD")
    public ResponseEntity<ViajeResponse> armar(@Valid @RequestBody ArmarViajeRequest request) {
        Comando comando = new Comando(request.cdId(), request.vehiculo(), request.paradas().stream()
                .map(p -> new ParadaPedida(p.cicloDespachoId(), p.ordenParada()))
                .toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(ViajeResponse.desde(armarViaje.armar(comando)));
    }

    @GetMapping
    @Operation(summary = "Listar viajes", description = "Con sus paradas; por defecto del más reciente al más "
            + "antiguo. Filtro opcional por estado. HU-47.")
    @ApiResponse(responseCode = "200", description = "Página de viajes")
    public Page<ViajeResponse> listar(
            @Parameter(description = "ARMADO, EN_CURSO o CERRADO") @RequestParam(required = false) EstadoViaje estado,
            @ParameterObject @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return consultarViajes.listar(estado, pageable).map(ViajeResponse::desde);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un viaje", description = "Con sus paradas en orden. HU-47.")
    @ApiResponse(responseCode = "200", description = "Viaje")
    @ApiResponse(responseCode = "404", description = "El viaje no existe")
    public ViajeResponse obtener(@PathVariable UUID id) {
        return ViajeResponse.desde(consultarViajes.obtener(id));
    }

    @PostMapping("/{id}/salida")
    @Operation(summary = "Registrar la salida del viaje",
            description = "El viaje debe estar ARMADO. Pasa a EN_CURSO, sus paradas a EN_TRANSITO y las órdenes de sus "
                    + "ciclos a EN_TRANSITO. fechaSalida es opcional (si falta, la hora del servidor); no puede ser "
                    + "futura (se toleran 2 minutos de reloj adelantado) ni anterior al despacho de sus ciclos. HU-48.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false, content = @Content(
            examples = @ExampleObject(value = "{\"fechaSalida\": \"2026-11-21T14:30:00Z\"}")))
    @ApiResponse(responseCode = "200", description = "Salida registrada")
    @ApiResponse(responseCode = "400", description = "La hora es posterior a la actual")
    @ApiResponse(responseCode = "404", description = "El viaje no existe")
    @ApiResponse(responseCode = "409", description = "El viaje no está ARMADO o alguna orden no está DESPACHADA")
    @ApiResponse(responseCode = "422", description = "La salida es anterior al despacho de alguno de sus ciclos")
    public ViajeResponse registrarSalida(@PathVariable UUID id,
                                         @RequestBody(required = false) RegistrarSalidaRequest request) {
        return ViajeResponse.desde(trayecto.registrarSalida(id, request == null ? null : request.fechaSalida()));
    }

    @PostMapping("/{id}/paradas/{paradaId}/llegada")
    @Operation(summary = "Registrar la llegada a una parada",
            description = "El viaje debe estar EN_CURSO y la parada EN_TRANSITO. La parada queda ENTREGADO; cuando "
                    + "todas lo están, el viaje pasa a CERRADO con la llegada más tardía como cierre. fechaLlegada "
                    + "es opcional (si falta, la hora del servidor); no puede ser futura (2 minutos de tolerancia) "
                    + "ni anterior a la salida. Registrar de nuevo una llegada responde 409. HU-48.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false, content = @Content(
            examples = @ExampleObject(value = "{\"fechaLlegada\": \"2026-11-21T16:05:00Z\"}")))
    @ApiResponse(responseCode = "200", description = "Llegada registrada")
    @ApiResponse(responseCode = "400", description = "La hora es posterior a la actual")
    @ApiResponse(responseCode = "404", description = "El viaje no existe o la parada no es de ese viaje")
    @ApiResponse(responseCode = "409", description = "El viaje no está EN_CURSO o la parada ya estaba ENTREGADO")
    @ApiResponse(responseCode = "422", description = "La llegada es anterior a la salida del viaje")
    public ViajeResponse registrarLlegada(@PathVariable UUID id, @PathVariable UUID paradaId,
                                          @RequestBody(required = false) RegistrarLlegadaRequest request) {
        return ViajeResponse.desde(trayecto.registrarLlegada(id, paradaId,
                request == null ? null : request.fechaLlegada()));
    }
}
