package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.CargaInicialComando;
import com.codefactory.supplychain.inventario.application.port.in.CargarInventarioInicialUseCase;
import com.codefactory.supplychain.inventario.application.port.in.ItemCargaInicial;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CargaInicialRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.CargaInicialResponse;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Carga inicial de inventario de un nodo (HU-37).
 *
 * El {@code @PreAuthorize} solo deja pasar a quien tiene alguno de los dos
 * scopes; cuál exige cada nodo (D-31) y el alcance por tienda los resuelve el
 * caso de uso, porque dependen del nodo.
 */
@RestController
@RequestMapping("/api/v1/nodos/{nodoId}/inventario")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('inventario:administrar', 'inventario-cd:administrar')")
public class CargaInicialController {

    private final CargarInventarioInicialUseCase cargarInventarioInicialUseCase;

    @PostMapping("/carga-inicial")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registrar la carga inicial de un nodo",
            description = "Lote de 1 a 500 ítems {sku, cantidad >= 0}, todo o nada. Solo crea filas "
                    + "nuevas: si alguna variante ya está cargada en el nodo se rechaza el lote completo. "
                    + "Los CD exigen inventario-cd:administrar; las tiendas, inventario:administrar.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lote cargado"),
            @ApiResponse(responseCode = "400", description = "Lote inválido, SKU repetidos o inexistentes"),
            @ApiResponse(responseCode = "401", description = "Token inválido o usuario inactivo"),
            @ApiResponse(responseCode = "403", description = "Sin el scope que exige el tipo de nodo"),
            @ApiResponse(responseCode = "404", description = "Nodo inexistente o fuera del alcance del usuario"),
            @ApiResponse(responseCode = "409", description = "Variantes ya cargadas en el nodo")
    })
    public CargaInicialResponse cargar(
            @PathVariable UUID nodoId,
            @Valid @RequestBody CargaInicialRequest request,
            @AuthenticationPrincipal UsuarioActual usuario) {
        var items = request.items().stream()
                .map(item -> new ItemCargaInicial(item.sku(), item.cantidad()))
                .toList();
        return CargaInicialResponse.desde(
                cargarInventarioInicialUseCase.cargar(new CargaInicialComando(nodoId, items, usuario)));
    }
}
