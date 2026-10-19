package com.codefactory.supplychain.ventas.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.ventas.application.port.out.AlmacenTiendaPort;
import com.codefactory.supplychain.ventas.application.port.out.TiendaVentaPort;
import com.codefactory.supplychain.inventario.api.ConsultaNodos;
import com.codefactory.supplychain.inventario.api.ConsultaTiendas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Capa anticorrupción hacia {@code inventario.api.ConsultaTiendas} y {@code ConsultaNodos}. */
@Component
@RequiredArgsConstructor
class TiendaVentaAdapter implements TiendaVentaPort, AlmacenTiendaPort {

    private final ConsultaTiendas consultaTiendas;
    private final ConsultaNodos consultaNodos;

    @Override
    public Optional<TiendaVenta> buscar(UUID tiendaId) {
        return consultaTiendas.buscar(tiendaId).flatMap(t ->
                consultaNodos.buscarAlmacen(tiendaId).map(almacen -> new TiendaVenta(t.id(), t.activa(), almacen)));
    }

    @Override
    public Optional<UUID> almacenDeTienda(UUID tiendaId) {
        return consultaNodos.buscarAlmacen(tiendaId);
    }
}
