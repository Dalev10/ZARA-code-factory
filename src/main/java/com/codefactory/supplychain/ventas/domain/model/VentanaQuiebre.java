package com.codefactory.supplychain.ventas.domain.model;

import com.codefactory.supplychain.ventas.domain.exception.VentanaInvalidaException;

import java.time.Instant;
import java.util.UUID;

/**
 * Periodo en que una variante estuvo sin existencias (stock &lt;= 0) en el Almacén de una tienda
 * (HU-45/37). Abierta mientras {@code fechaFin} sea nula; {@code fechaFin} y {@code motivoCierre}
 * van siempre juntos (CHECK de V20).
 */
public class VentanaQuiebre {

    private final UUID id;
    private final UUID tiendaId;
    private final UUID varianteId;
    private final Instant fechaInicio;
    private Instant fechaFin;
    private MotivoCierreVentana motivoCierre;

    private VentanaQuiebre(UUID id, UUID tiendaId, UUID varianteId, Instant fechaInicio,
                           Instant fechaFin, MotivoCierreVentana motivoCierre) {
        this.id = id;
        this.tiendaId = tiendaId;
        this.varianteId = varianteId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.motivoCierre = motivoCierre;
    }

    public static VentanaQuiebre abrir(UUID tiendaId, UUID varianteId, Instant fechaInicio) {
        if (tiendaId == null || varianteId == null || fechaInicio == null) {
            throw new VentanaInvalidaException("Tienda, variante y fecha de inicio son obligatorias");
        }
        return new VentanaQuiebre(UUID.randomUUID(), tiendaId, varianteId, fechaInicio, null, null);
    }

    public static VentanaQuiebre reconstituir(UUID id, UUID tiendaId, UUID varianteId, Instant fechaInicio,
                                               Instant fechaFin, MotivoCierreVentana motivoCierre) {
        return new VentanaQuiebre(id, tiendaId, varianteId, fechaInicio, fechaFin, motivoCierre);
    }

    /** Si el evento llega con una fecha anterior al inicio, el cierre se ajusta al inicio (duración cero). */
    public void cerrar(Instant fecha, MotivoCierreVentana motivo) {
        if (!estaAbierta()) {
            throw new VentanaInvalidaException("La ventana ya está cerrada");
        }
        if (fecha == null || motivo == null) {
            throw new VentanaInvalidaException("Fecha y motivo de cierre son obligatorios");
        }
        this.fechaFin = fecha.isBefore(fechaInicio) ? fechaInicio : fecha;
        this.motivoCierre = motivo;
    }

    /** Corrección hecha por la reconciliación sobre una ventana cerrada con fecha errónea. */
    public void corregirCierre(Instant fecha, MotivoCierreVentana motivo) {
        if (fecha == null || motivo == null || fecha.isBefore(fechaInicio)) {
            throw new VentanaInvalidaException("Cierre inválido");
        }
        this.fechaFin = fecha;
        this.motivoCierre = motivo;
    }

    public boolean estaAbierta() {
        return fechaFin == null;
    }

    public UUID getId() { return id; }
    public UUID getTiendaId() { return tiendaId; }
    public UUID getVarianteId() { return varianteId; }
    public Instant getFechaInicio() { return fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }
    public MotivoCierreVentana getMotivoCierre() { return motivoCierre; }
}
