package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.out.CicloDespachoRepository;
import com.codefactory.supplychain.logistica.application.port.out.InventarioDespachoPort;
import com.codefactory.supplychain.logistica.application.port.out.OrdenDespachoPort;
import com.codefactory.supplychain.logistica.application.port.out.OrdenDespachoPort.OrdenADespachar;
import com.codefactory.supplychain.logistica.domain.model.CicloDespacho;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Lógica transaccional de HU-46. Orden, ciclo e inventario cambian en una sola transacción:
 * si el CD no tiene nada a la mano, la excepción revierte también el paso a EN_DESPACHO.
 * <p>
 * Sale {@code D = min(pendiente, a_la_mano)} y se libera {@code min(D, reservaRestante)} de la
 * reserva: al despachar de nuevo tras una reapertura la reserva ya se consumió (D-92). El
 * reintento vive en {@link DespacharOrdenRetryableService}, fuera de esta transacción.
 *
 * Módulo: logistica.
 */
@Service
public class DespacharOrdenService {

    private final OrdenDespachoPort ordenes;
    private final InventarioDespachoPort inventario;
    private final CicloDespachoRepository ciclos;
    private final Clock reloj;

    public DespacharOrdenService(OrdenDespachoPort ordenes, InventarioDespachoPort inventario,
                                 CicloDespachoRepository ciclos, Clock reloj) {
        this.ordenes = ordenes;
        this.inventario = inventario;
        this.ciclos = ciclos;
        this.reloj = reloj;
    }

    @Transactional
    public CicloDespacho despachar(UUID ordenId) {
        OrdenADespachar orden = ordenes.iniciarDespacho(ordenId);
        int aLaMano = inventario.aLaManoEnCd(orden.cdId(), orden.varianteId());
        CicloDespacho ciclo = ciclos.guardar(CicloDespacho.despachar(ordenId, ciclos.ultimoNumeroCiclo(ordenId) + 1,
                orden.cantidadSolicitadaCiclo(), aLaMano, Instant.now(reloj)));
        int despachada = ciclo.getCantidadDespachada();
        inventario.registrarDespacho(orden.cdId(), orden.varianteId(), despachada,
                Math.min(despachada, orden.reservaRestante()), ciclo.getId());
        ordenes.confirmarDespacho(ordenId, despachada);
        return ciclo;
    }
}
