package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web;

import com.codefactory.supplychain.logistica.application.port.in.ArmarViajeUseCase;
import com.codefactory.supplychain.logistica.application.port.in.ArmarViajeUseCase.Comando;
import com.codefactory.supplychain.logistica.application.port.in.ArmarViajeUseCase.ParadaPedida;
import com.codefactory.supplychain.logistica.application.port.in.ConsultarViajesUseCase;
import com.codefactory.supplychain.logistica.domain.model.EstadoViaje;
import com.codefactory.supplychain.logistica.infrastructure.adapter.in.web.dto.ArmarViajeRequest;
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
 * Viajes del CD a las tiendas (HU-47; la salida y la llegada son HU-48). Scope
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
}
