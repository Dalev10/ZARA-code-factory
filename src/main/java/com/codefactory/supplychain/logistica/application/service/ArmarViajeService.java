package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.in.ArmarViajeUseCase.Comando;
import com.codefactory.supplychain.logistica.application.port.in.ArmarViajeUseCase.ParadaPedida;
import com.codefactory.supplychain.logistica.application.port.out.CentrosDistribucionPort;
import com.codefactory.supplychain.logistica.application.port.out.CicloDespachoRepository;
import com.codefactory.supplychain.logistica.application.port.out.OrdenViajePort;
import com.codefactory.supplychain.logistica.application.port.out.OrdenViajePort.DestinoOrden;
import com.codefactory.supplychain.logistica.application.port.out.ViajeRepository;
import com.codefactory.supplychain.logistica.domain.exception.CdNoEncontradoException;
import com.codefactory.supplychain.logistica.domain.exception.CicloDeOtroCdException;
import com.codefactory.supplychain.logistica.domain.exception.CicloDespachoNoEncontradoException;
import com.codefactory.supplychain.logistica.domain.exception.CicloYaEnViajeException;
import com.codefactory.supplychain.logistica.domain.model.CicloDespacho;
import com.codefactory.supplychain.logistica.domain.model.Viaje;
import com.codefactory.supplychain.logistica.domain.model.Viaje.NuevaParada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica transaccional de HU-47. La tienda de cada parada sale de la orden de su ciclo; el
 * ciclo debe salir del CD del viaje (422) y no estar en otro viaje (409). El
 * {@code UNIQUE (ciclo_despacho_id)} cubre la carrera entre dos viajes que cargan el mismo
 * ciclo: el reintento de {@link ArmarViajeRetryableService} encuentra el ciclo ya cargado.
 *
 * Módulo: logistica.
 */
@Service
public class ArmarViajeService {

    private final CentrosDistribucionPort cds;
    private final CicloDespachoRepository ciclos;
    private final OrdenViajePort ordenes;
    private final ViajeRepository viajes;
    private final Clock reloj;

    public ArmarViajeService(CentrosDistribucionPort cds, CicloDespachoRepository ciclos, OrdenViajePort ordenes,
                             ViajeRepository viajes, Clock reloj) {
        this.cds = cds;
        this.ciclos = ciclos;
        this.ordenes = ordenes;
        this.viajes = viajes;
        this.reloj = reloj;
    }

    @Transactional
    public Viaje armar(Comando comando) {
        if (!cds.existe(comando.cdId())) {
            throw new CdNoEncontradoException(comando.cdId());
        }
        List<NuevaParada> nuevas = new ArrayList<>();
        for (ParadaPedida pedida : comando.paradas() == null ? List.<ParadaPedida>of() : comando.paradas()) {
            CicloDespacho ciclo = ciclos.buscarPorId(pedida.cicloDespachoId())
                    .orElseThrow(() -> new CicloDespachoNoEncontradoException(pedida.cicloDespachoId()));
            if (viajes.cicloEnViaje(ciclo.getId())) {
                throw new CicloYaEnViajeException(ciclo.getId());
            }
            DestinoOrden destino = ordenes.consultar(ciclo.getOrdenId());
            if (!comando.cdId().equals(destino.cdId())) {
                throw new CicloDeOtroCdException(ciclo.getId(), comando.cdId());
            }
            nuevas.add(new NuevaParada(ciclo.getId(), destino.tiendaId(), pedida.ordenParada()));
        }
        return viajes.guardar(Viaje.armar(comando.cdId(), comando.vehiculo(), nuevas, Instant.now(reloj)));
    }
}
