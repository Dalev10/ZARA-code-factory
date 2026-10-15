package com.codefactory.supplychain.identity.infrastructure.adapter.out.inventario;

import com.codefactory.supplychain.identity.application.port.out.TiendaAsignable;
import com.codefactory.supplychain.identity.application.port.out.TiendaConsultaPort;
import com.codefactory.supplychain.inventario.api.ConsultaTiendas;
import com.codefactory.supplychain.inventario.api.TiendaResumen;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Capa anticorrupción: único punto de identity que conoce la API de inventario. Si esa API
 * cambia, solo cambia este adaptador.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
@RequiredArgsConstructor
public class TiendaConsultaAdapter implements TiendaConsultaPort {

    private final ConsultaTiendas consultaTiendas;

    @Override
    public Optional<TiendaAsignable> buscar(UUID tiendaId) {
        return consultaTiendas.buscar(tiendaId).map(TiendaConsultaAdapter::traducir);
    }

    @Override
    public List<TiendaAsignable> buscarTodas(Collection<UUID> tiendaIds) {
        return consultaTiendas.buscarTodas(tiendaIds).stream().map(TiendaConsultaAdapter::traducir).toList();
    }

    private static TiendaAsignable traducir(TiendaResumen tienda) {
        return new TiendaAsignable(tienda.id(), tienda.nombre(), tienda.activa());
    }
}
