package com.codefactory.supplychain.logistica.domain.model;

import com.codefactory.supplychain.logistica.domain.exception.CantidadRecibidaExcedeDespachoException;
import com.codefactory.supplychain.logistica.domain.exception.RecepcionInvalidaException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Recepción de una parada en la Bodega_Tienda (HU-49). Hay una sola por parada: el reintento
 * devuelve la original (D-91).
 * <p>
 * Lo no recibido es <b>pérdida en tránsito</b>: queda como {@code discrepancia =
 * despachada − recibida}, sin movimiento de kardex (D-90). Se cumple siempre
 * {@code 0 ≤ recibida ≤ despachada}, así que la discrepancia nunca es negativa.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Recepcion {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID viajeParadaId;
    private final UUID cicloDespachoId;
    private final int cantidadRecibida;
    private final int discrepancia;
    private final UUID registradoPor;
    private final Instant fechaRecepcion;

    private Recepcion(UUID id, UUID viajeParadaId, UUID cicloDespachoId, int cantidadRecibida,
                      int discrepancia, UUID registradoPor, Instant fechaRecepcion) {
        exigirPresente(viajeParadaId, "La recepción debe indicar la parada");
        exigirPresente(cicloDespachoId, "La recepción debe indicar el ciclo de despacho");
        exigirPresente(registradoPor, "La recepción debe indicar quién la registra");
        exigirPresente(fechaRecepcion, "La recepción debe indicar la fecha");
        if (cantidadRecibida < 0) {
            throw new RecepcionInvalidaException("La cantidad recibida no puede ser negativa");
        }
        if (discrepancia < 0) {
            throw new RecepcionInvalidaException("La discrepancia no puede ser negativa");
        }
        this.id = id;
        this.viajeParadaId = viajeParadaId;
        this.cicloDespachoId = cicloDespachoId;
        this.cantidadRecibida = cantidadRecibida;
        this.discrepancia = discrepancia;
        this.registradoPor = registradoPor;
        this.fechaRecepcion = fechaRecepcion;
    }

    /**
     * Registra lo recibido de una parada contra lo despachado en su ciclo.
     *
     * @throws RecepcionInvalidaException              si alguna cantidad es negativa o falta un dato
     * @throws CantidadRecibidaExcedeDespachoException si se recibe más de lo despachado
     */
    public static Recepcion registrar(UUID viajeParadaId, UUID cicloDespachoId, int cantidadDespachada,
                                      int cantidadRecibida, UUID registradoPor, Instant ahora) {
        if (cantidadDespachada < 0) {
            throw new RecepcionInvalidaException("La cantidad despachada no puede ser negativa");
        }
        if (cantidadRecibida > cantidadDespachada) {
            throw new CantidadRecibidaExcedeDespachoException(cantidadRecibida, cantidadDespachada);
        }
        return new Recepcion(UUID.randomUUID(), viajeParadaId, cicloDespachoId, cantidadRecibida,
                cantidadDespachada - cantidadRecibida, registradoPor, ahora);
    }

    public static Recepcion reconstruir(UUID id, UUID viajeParadaId, UUID cicloDespachoId, int cantidadRecibida,
                                        int discrepancia, UUID registradoPor, Instant fechaRecepcion) {
        return new Recepcion(id, viajeParadaId, cicloDespachoId, cantidadRecibida, discrepancia,
                registradoPor, fechaRecepcion);
    }

    /** {@code true} si algo llegó; si no, no hay nada que sumar a la Bodega_Tienda. */
    public boolean recibioMercancia() {
        return cantidadRecibida > 0;
    }

    /** {@code true} si hubo pérdida en tránsito. */
    public boolean tieneDiscrepancia() {
        return discrepancia > 0;
    }

    /** Lo que salió del CD en el ciclo: {@code recibida + discrepancia}. */
    public int cantidadDespachada() {
        return cantidadRecibida + discrepancia;
    }

    private static void exigirPresente(Object valor, String mensaje) {
        if (valor == null) {
            throw new RecepcionInvalidaException(mensaje);
        }
    }
}
