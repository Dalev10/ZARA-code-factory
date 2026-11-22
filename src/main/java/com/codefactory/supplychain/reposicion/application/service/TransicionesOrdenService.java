package com.codefactory.supplychain.reposicion.application.service;

import com.codefactory.supplychain.reposicion.application.port.in.TransicionesOrdenUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.OrdenReabastecimientoRepository;
import com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Transiciones de la orden. Carga, aplica la regla del dominio y guarda. La
 * versión de la orden viaja con ella: si otra transacción la cambió, el
 * {@code @Version} hace fallar la confirmación y el llamador reintenta fuera.
 */
@Service
@Transactional
public class TransicionesOrdenService implements TransicionesOrdenUseCase {

    private final OrdenReabastecimientoRepository ordenes;

    public TransicionesOrdenService(OrdenReabastecimientoRepository ordenes) {
        this.ordenes = ordenes;
    }

    @Override
    public OrdenReabastecimiento iniciarDespacho(UUID ordenId) {
        return aplicar(ordenId, OrdenReabastecimiento::iniciarDespacho);
    }

    @Override
    public OrdenReabastecimiento confirmarDespacho(UUID ordenId, int cantidadDespachada) {
        return aplicar(ordenId, orden -> orden.confirmarDespacho(cantidadDespachada));
    }

    @Override
    public OrdenReabastecimiento marcarEnTransito(UUID ordenId) {
        return aplicar(ordenId, OrdenReabastecimiento::marcarEnTransito);
    }

    @Override
    public OrdenReabastecimiento registrarRecepcion(UUID ordenId, int cantidadRecibida) {
        return aplicar(ordenId, orden -> orden.registrarRecepcion(cantidadRecibida));
    }

    private OrdenReabastecimiento aplicar(UUID ordenId, Consumer<OrdenReabastecimiento> transicion) {
        OrdenReabastecimiento orden = ordenes.buscarPorId(ordenId)
                .orElseThrow(() -> ReposicionRecursoNoEncontradoException.orden(ordenId));
        transicion.accept(orden);
        return ordenes.guardar(orden);
    }
}
