package com.codefactory.supplychain.logistica.domain.model;

import com.codefactory.supplychain.logistica.domain.exception.CdSinStockParaDespacharException;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Un ciclo de despacho de una orden de reabastecimiento (HU-46): lo que realmente salió del CD.
 * Se despacha lo que hay a la mano, hasta lo solicitado; el resultado, el pendiente y el fill
 * rate se derivan de las dos cantidades. Inmutable una vez creado.
 *
 * Módulo: logistica.
 */
@Getter
public final class CicloDespacho {

    private final UUID id;
    private final UUID ordenId;
    private final int numeroCiclo;
    private final int cantidadSolicitada;
    private final int cantidadDespachada;
    private final Instant fechaDespacho;

    private CicloDespacho(UUID id, UUID ordenId, int numeroCiclo, int cantidadSolicitada, int cantidadDespachada,
                          Instant fechaDespacho) {
        this.id = id;
        this.ordenId = ordenId;
        this.numeroCiclo = numeroCiclo;
        this.cantidadSolicitada = cantidadSolicitada;
        this.cantidadDespachada = cantidadDespachada;
        this.fechaDespacho = fechaDespacho;
    }

    /**
     * Despacha {@code min(cantidadSolicitada, aLaManoCd)}.
     *
     * @throws CdSinStockParaDespacharException si el CD no tiene nada a la mano
     */
    public static CicloDespacho despachar(UUID ordenId, int numeroCiclo, int cantidadSolicitada, int aLaManoCd,
                                          Instant ahora) {
        if (cantidadSolicitada <= 0) {
            throw new IllegalStateException("La orden " + ordenId + " no tiene cantidad por despachar");
        }
        int despachada = Math.min(cantidadSolicitada, Math.max(aLaManoCd, 0));
        if (despachada == 0) {
            throw new CdSinStockParaDespacharException(ordenId);
        }
        return new CicloDespacho(UUID.randomUUID(), ordenId, numeroCiclo, cantidadSolicitada, despachada, ahora);
    }

    public static CicloDespacho reconstruir(UUID id, UUID ordenId, int numeroCiclo, int cantidadSolicitada,
                                            int cantidadDespachada, Instant fechaDespacho) {
        return new CicloDespacho(id, ordenId, numeroCiclo, cantidadSolicitada, cantidadDespachada, fechaDespacho);
    }

    public int getCantidadPendiente() {
        return cantidadSolicitada - cantidadDespachada;
    }

    public ResultadoDespacho getResultado() {
        return getCantidadPendiente() == 0 ? ResultadoDespacho.COMPLETO : ResultadoDespacho.PARCIAL;
    }

    /** Despachado entre solicitado del ciclo, con cuatro decimales (1.0000 = completo). */
    public BigDecimal getFillRate() {
        return BigDecimal.valueOf(cantidadDespachada)
                .divide(BigDecimal.valueOf(cantidadSolicitada), 4, RoundingMode.HALF_UP);
    }
}
