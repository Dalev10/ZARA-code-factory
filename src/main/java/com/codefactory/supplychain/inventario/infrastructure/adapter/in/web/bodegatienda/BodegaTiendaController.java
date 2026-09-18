package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.bodegatienda;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.codefactory.supplychain.inventario.application.port.in.bodegatienda.ConsultarBodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.application.port.in.bodegatienda.EliminarBodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.application.port.in.bodegatienda.ModificarBodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.application.port.in.bodegatienda.RegistrarBodegaTiendaUseCase;
import com.codefactory.supplychain.inventario.domain.model.bodegatienda.BodegaTienda;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.bodegatienda.dto.BodegaTiendaResponse;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.bodegatienda.dto.ModificarBodegaTiendaRequest;
import com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.bodegatienda.dto.RegistrarBodegaTiendaRequest;

/**
 * Adaptador HTTP para los casos de uso de BodegaTienda.
 */
@RestController
@RequestMapping("/api/inventario/bodegas-tiendas")
public class BodegaTiendaController {

    private final RegistrarBodegaTiendaUseCase registrarUseCase;
    private final ConsultarBodegaTiendaUseCase consultarUseCase;
    private final ModificarBodegaTiendaUseCase modificarUseCase;
    private final EliminarBodegaTiendaUseCase eliminarUseCase;

    public BodegaTiendaController(
            RegistrarBodegaTiendaUseCase registrarUseCase,
            ConsultarBodegaTiendaUseCase consultarUseCase,
            ModificarBodegaTiendaUseCase modificarUseCase,
            EliminarBodegaTiendaUseCase eliminarUseCase) {
        this.registrarUseCase = registrarUseCase;
        this.consultarUseCase = consultarUseCase;
        this.modificarUseCase = modificarUseCase;
        this.eliminarUseCase = eliminarUseCase;
    }

    @PostMapping
    public ResponseEntity<BodegaTiendaResponse> registrar(
            @Valid @RequestBody RegistrarBodegaTiendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(registrarUseCase.registrar(request.tiendaId())));
    }

    @GetMapping("/{id}")
    public BodegaTiendaResponse consultarPorId(@PathVariable Long id) {
        return toResponse(consultarUseCase.consultarPorId(id));
    }

    @GetMapping("/tienda/{tiendaId}")
    public BodegaTiendaResponse consultarPorTiendaId(@PathVariable Long tiendaId) {
        return toResponse(consultarUseCase.consultarPorTiendaId(tiendaId));
    }

    @PutMapping("/{id}")
    public BodegaTiendaResponse modificar(
            @PathVariable Long id,
            @Valid @RequestBody ModificarBodegaTiendaRequest request) {
        BodegaTienda bodegaTienda = BodegaTienda.crear(request.tiendaId());
        return toResponse(modificarUseCase.modificar(id, bodegaTienda));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        eliminarUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private static BodegaTiendaResponse toResponse(BodegaTienda bodegaTienda) {
        return BodegaTiendaResponse.from(bodegaTienda);
    }
}
