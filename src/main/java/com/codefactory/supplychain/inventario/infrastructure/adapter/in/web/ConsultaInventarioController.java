package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.application.port.in.ConsultaKardexComando;
import com.codefactory.supplychain.inventario.application.port.in.ConsultarInventarioUseCase;
import com.codefactory.supplychain.inventario.domain.model.TipoMovimientoInventario;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.MovimientoKardexResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto.StockResponse;
import com.codefactory.supplychain.shared.security.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consultas de inventario (HU-39, D-33). Todas exigen {@code inventario:consultar};
 * el alcance por tienda lo resuelve el caso de uso.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inventario:consultar')")
public class ConsultaInventarioController {

    private final ConsultarInventarioUseCase consultarInventarioUseCase;

    @GetMapping("/nodos/{nodoId}/inventario")
    @Operation(
            summary = "Consultar el stock de un nodo",
            description = "Existencias de todas las variantes cargadas en el nodo, paginadas y ordenadas "
                    + "por variante. Un usuario sin acceso global solo ve los nodos de sus tiendas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de existencias"),
            @ApiResponse(responseCode = "401", description = "Token inválido o usuario inactivo"),
            @ApiResponse(responseCode = "403", description = "Sin el scope inventario:consultar"),
            @ApiResponse(responseCode = "404", description = "Nodo inexistente o fuera del alcance del usuario")
    })
    public Page<StockResponse> stockDeNodo(
            @PathVariable UUID nodoId,
            @AuthenticationPrincipal UsuarioActual usuario,
            Pageable pageable) {
        return consultarInventarioUseCase.stockDeNodo(nodoId, usuario, pageable)
                .map(StockResponse::desde);
    }

    @GetMapping("/variantes/{varianteId}/inventario")
    @Operation(
            summary = "Consultar el stock de una variante",
            description = "Existencias de la variante en cada nodo visible para el usuario. Un nodo fuera "
                    + "de su alcance no aparece; si no hay ninguno visible, la lista va vacía.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Existencias por nodo visible"),
            @ApiResponse(responseCode = "401", description = "Token inválido o usuario inactivo"),
            @ApiResponse(responseCode = "403", description = "Sin el scope inventario:consultar")
    })
    public List<StockResponse> stockDeVariante(
            @PathVariable UUID varianteId,
            @AuthenticationPrincipal UsuarioActual usuario) {
        return consultarInventarioUseCase.stockDeVariante(varianteId, usuario).stream()
                .map(StockResponse::desde)
                .toList();
    }

    @GetMapping("/nodos/{nodoId}/inventario/movimientos")
    @Operation(
            summary = "Consultar el kardex de un nodo",
            description = "Movimientos del nodo ordenados por fecha ascendente, con filtros opcionales por "
                    + "tipo y por rango de fechas (inclusive). Paginado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página del kardex"),
            @ApiResponse(responseCode = "400", description = "Tipo de movimiento desconocido o fecha inválida"),
            @ApiResponse(responseCode = "401", description = "Token inválido o usuario inactivo"),
            @ApiResponse(responseCode = "403", description = "Sin el scope inventario:consultar"),
            @ApiResponse(responseCode = "404", description = "Nodo inexistente o fuera del alcance del usuario")
    })
    public Page<MovimientoKardexResponse> kardex(
            @PathVariable UUID nodoId,
            @RequestParam(required = false) TipoMovimientoInventario tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @AuthenticationPrincipal UsuarioActual usuario,
            Pageable pageable) {
        var comando = new ConsultaKardexComando(nodoId, tipo, desde, hasta, usuario);
        return consultarInventarioUseCase.kardex(comando, pageable)
                .map(MovimientoKardexResponse::desde);
    }
}
