package com.codefactory.supplychain.ventas.domain.model;

import com.codefactory.supplychain.ventas.domain.exception.SolicitudInvalidaException;
import com.codefactory.supplychain.ventas.domain.exception.SolicitudYaAnuladaException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Demanda insatisfecha registrada en una tienda (HU-43/35). No valida quiebre: guarda el stock que el
 * sistema tenía en el Almacén al registrarla ({@code stockSistema}, nulo si la variante no tiene carga
 * inicial en el Almacén), para distinguir quiebres reales de inventario fantasma.
 */
public class SolicitudNoSatisfecha {

    public static final int LONGITUD_MAXIMA_CLAVE = 100;

    private final UUID id;
    private final UUID tiendaId;
    private final UUID varianteId;
    private final int cantidad;
    private final Integer stockSistema;
    private final Instant registradaEn;
    private final UUID registradoPor;
    private final String claveIdempotencia;
    private EstadoSolicitud estado;
    private UUID anuladaPor;
    private Instant anuladaEn;

    private SolicitudNoSatisfecha(UUID id, UUID tiendaId, UUID varianteId, int cantidad, Integer stockSistema,
                                  Instant registradaEn, UUID registradoPor, String claveIdempotencia,
                                  EstadoSolicitud estado, UUID anuladaPor, Instant anuladaEn) {
        this.id = id;
        this.tiendaId = tiendaId;
        this.varianteId = varianteId;
        this.cantidad = cantidad;
        this.stockSistema = stockSistema;
        this.registradaEn = registradaEn;
        this.registradoPor = registradoPor;
        this.claveIdempotencia = claveIdempotencia;
        this.estado = estado;
        this.anuladaPor = anuladaPor;
        this.anuladaEn = anuladaEn;
    }

    public static SolicitudNoSatisfecha registrar(UUID tiendaId, UUID varianteId, int cantidad, Integer stockSistema,
                                                   Instant registradaEn, UUID registradoPor, String claveIdempotencia) {
        if (tiendaId == null) {
            throw new SolicitudInvalidaException("La tienda es obligatoria");
        }
        if (varianteId == null) {
            throw new SolicitudInvalidaException("La variante es obligatoria");
        }
        if (registradoPor == null) {
            throw new SolicitudInvalidaException("El usuario que registra es obligatorio");
        }
        if (cantidad <= 0) {
            throw new SolicitudInvalidaException("La cantidad solicitada debe ser mayor que cero");
        }
        if (claveIdempotencia == null || claveIdempotencia.isBlank()) {
            throw new SolicitudInvalidaException("La clave de idempotencia es obligatoria");
        }
        if (claveIdempotencia.length() > LONGITUD_MAXIMA_CLAVE) {
            throw new SolicitudInvalidaException(
                    "La clave de idempotencia no puede superar " + LONGITUD_MAXIMA_CLAVE + " caracteres");
        }
        return new SolicitudNoSatisfecha(UUID.randomUUID(), tiendaId, varianteId, cantidad, stockSistema,
                registradaEn, registradoPor, claveIdempotencia, EstadoSolicitud.ACTIVA, null, null);
    }

    public static SolicitudNoSatisfecha reconstituir(UUID id, UUID tiendaId, UUID varianteId, int cantidad,
                                                      Integer stockSistema, Instant registradaEn, UUID registradoPor,
                                                      String claveIdempotencia, EstadoSolicitud estado,
                                                      UUID anuladaPor, Instant anuladaEn) {
        return new SolicitudNoSatisfecha(id, tiendaId, varianteId, cantidad, stockSistema, registradaEn,
                registradoPor, claveIdempotencia, estado, anuladaPor, anuladaEn);
    }

    /** No toca el stock: anular una solicitud solo la excluye del dataset de demanda. */
    public void anular(UUID usuarioId, Instant ahora) {
        if (estado == EstadoSolicitud.ANULADA) {
            throw new SolicitudYaAnuladaException(id);
        }
        if (usuarioId == null) {
            throw new SolicitudInvalidaException("El usuario que anula es obligatorio");
        }
        this.estado = EstadoSolicitud.ANULADA;
        this.anuladaPor = usuarioId;
        this.anuladaEn = ahora;
    }

    /** Una repetición legítima: misma clave, mismo autor y mismos datos. */
    public boolean esRepeticionDe(UUID usuarioId, UUID tiendaId, UUID varianteId, int cantidad) {
        return Objects.equals(registradoPor, usuarioId) && this.tiendaId.equals(tiendaId)
                && this.varianteId.equals(varianteId) && this.cantidad == cantidad;
    }

    public UUID getId() { return id; }
    public UUID getTiendaId() { return tiendaId; }
    public UUID getVarianteId() { return varianteId; }
    public int getCantidad() { return cantidad; }
    public Integer getStockSistema() { return stockSistema; }
    public Instant getRegistradaEn() { return registradaEn; }
    public UUID getRegistradoPor() { return registradoPor; }
    public String getClaveIdempotencia() { return claveIdempotencia; }
    public EstadoSolicitud getEstado() { return estado; }
    public UUID getAnuladaPor() { return anuladaPor; }
    public Instant getAnuladaEn() { return anuladaEn; }
}
