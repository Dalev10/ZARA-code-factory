package com.codefactory.supplychain.logistica.application.service;

import com.codefactory.supplychain.logistica.application.port.in.RegistrarRecepcionUseCase.Comando;
import com.codefactory.supplychain.logistica.application.port.in.RegistrarRecepcionUseCase.ResultadoRecepcion;
import com.codefactory.supplychain.logistica.application.port.out.InventarioRecepcionPort;
import com.codefactory.supplychain.logistica.application.port.out.OrdenRecepcionPort;
import com.codefactory.supplychain.logistica.application.port.out.OrdenRecepcionPort.Situacion;
import com.codefactory.supplychain.logistica.application.port.out.ParadasRecepcionPort;
import com.codefactory.supplychain.logistica.application.port.out.RecepcionRepository;
import com.codefactory.supplychain.logistica.domain.exception.ParadaNoEncontradaException;
import com.codefactory.supplychain.logistica.domain.exception.ParadaNoEntregadaException;
import com.codefactory.supplychain.logistica.domain.exception.RecepcionInvalidaException;
import com.codefactory.supplychain.logistica.domain.model.ParadaRecepcion;
import com.codefactory.supplychain.logistica.domain.model.Recepcion;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Lógica transaccional de HU-49. La recepción, la suma en la Bodega_Tienda y el cambio de la
 * orden ocurren en una sola transacción: si uno falla, no queda ninguno.
 * <p>
 * La recepción se escribe <b>antes</b> de mover el inventario y la orden. Así, dos registros
 * simultáneos de la misma parada chocan en el {@code UNIQUE (viaje_parada_id)} y el segundo
 * falla sin haber tocado nada. El reintento vive en {@link RegistrarRecepcionRetryableService},
 * fuera de esta transacción, y en el segundo intento encuentra la recepción original (D-91).
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Service
public class RegistrarRecepcionService {

    private final ParadasRecepcionPort paradas;
    private final RecepcionRepository recepciones;
    private final InventarioRecepcionPort inventario;
    private final OrdenRecepcionPort ordenes;
    private final Clock reloj;

    public RegistrarRecepcionService(ParadasRecepcionPort paradas, RecepcionRepository recepciones,
                                     InventarioRecepcionPort inventario, OrdenRecepcionPort ordenes,
                                     ObjectProvider<Clock> reloj) {
        this.paradas = paradas;
        this.recepciones = recepciones;
        this.inventario = inventario;
        this.ordenes = ordenes;
        this.reloj = reloj.getIfAvailable(Clock::systemUTC);
    }

    @Transactional
    public ResultadoRecepcion registrar(Comando comando) {
        validar(comando);
        ParadaRecepcion parada = paradas.buscar(comando.paradaId())
                .filter(p -> p.viajeId().equals(comando.viajeId()))
                .filter(p -> comando.actor().puedeOperarEn(p.tiendaId()))
                .orElseThrow(() -> new ParadaNoEncontradaException(comando.paradaId()));

        Optional<Recepcion> existente = recepciones.buscarPorParada(parada.id());
        if (existente.isPresent()) {
            Situacion situacion = ordenes.consultar(parada.ordenId()).situacion();
            return new ResultadoRecepcion(false, existente.get(), situacion.estado(), situacion.cantidadPendiente());
        }
        if (!parada.entregada()) {
            throw new ParadaNoEntregadaException(parada.id());
        }

        Recepcion recepcion = recepciones.guardar(Recepcion.registrar(parada.id(), parada.cicloDespachoId(),
                parada.cantidadDespachada(), comando.cantidadRecibida(), comando.actor().id(), Instant.now(reloj)));

        if (recepcion.recibioMercancia()) {
            var orden = ordenes.consultar(parada.ordenId());
            inventario.sumarRecibido(parada.tiendaId(), orden.varianteId(), recepcion.getCantidadRecibida(),
                    recepcion.getId());
        }
        Situacion situacion = ordenes.registrarRecepcion(parada.ordenId(), recepcion.getCantidadRecibida());
        return new ResultadoRecepcion(true, recepcion, situacion.estado(), situacion.cantidadPendiente());
    }

    private static void validar(Comando c) {
        if (c == null || c.actor() == null || c.viajeId() == null || c.paradaId() == null) {
            throw new RecepcionInvalidaException("Usuario, viaje y parada son obligatorios");
        }
    }
}
