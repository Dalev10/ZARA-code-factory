package com.codefactory.supplychain.reposicion.application.port.in;

import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;

import java.util.UUID;

/**
 * Transiciones del ciclo de la orden que piden otros módulos.
 * Se unen a la transacción del llamador y no reintentan.
 *
 * Módulo: reposicion.
 */
public interface TransicionesOrdenUseCase {

    /**
     * @throws com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException
     *         si la orden no existe
     * @throws com.codefactory.supplychain.reposicion.domain.exception.TransicionOrdenInvalidaException
     *         si su estado no permite la transición
     */
    OrdenReabastecimiento iniciarDespacho(UUID ordenId);

    OrdenReabastecimiento confirmarDespacho(UUID ordenId, int cantidadDespachada);

    OrdenReabastecimiento marcarEnTransito(UUID ordenId);

    OrdenReabastecimiento registrarRecepcion(UUID ordenId, int cantidadRecibida);
}
