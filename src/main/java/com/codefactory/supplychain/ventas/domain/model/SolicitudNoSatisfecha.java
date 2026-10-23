package com.codefactory.supplychain.ventas.domain.model;

import com.codefactory.supplychain.ventas.domain.exception.SolicitudInvalidaException;
import com.codefactory.supplychain.ventas.domain.exception.SolicitudYaAnuladaException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Demanda insatisfecha registrada en una tienda (HU-43, HU-44). No valida quiebre: guarda el stock que el
 * sistema tenía en el Almacén al registrarla ({@code stockSistema}, 0 si la variante no tiene carga
 * inicial en el Almacén), para distinguir quiebres reales de inventario fantasma.
 */
public class SolicitudNoSatisfecha {

    public static final int LONGITUD_MAXIMA_CLAVE = 100;
    public static final int LONGITUD_MAXIMA_MOTIVO = 500;

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
    private String motivoAnulacion;

    private SolicitudNoSatisfecha(UUID id, UUID tiendaId, UUID varianteId, int cantidad, Integer stockSistema,
                                  Instant registradaEn, UUID registradoPor, String claveIdempotencia,
                                  EstadoSolicitud estado, UUID anuladaPor, Instant anuladaEn,
                                  String motivoAnulacion) {
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
        this.motivoAnulacion = motivoAnulacion;
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
                registradaEn, registradoPor, claveIdempotencia, EstadoSolicitud.ACTIVA, null, null, null);
    }

    public static SolicitudNoSatisfecha reconstituir(UUID id, UUID tiendaId, UUID varianteId, int cantidad,
                                                      Integer stockSistema, Instant registradaEn, UUID registradoPor,
                                                      String claveIdempotencia, EstadoSolicitud estado,
                                                      UUID anuladaPor, Instant anuladaEn, String motivoAnulacion) {
        return new SolicitudNoSatisfecha(id, tiendaId, varianteId, cantidad, stockSistema, registradaEn,
                registradoPor, claveIdempotencia, estado, anuladaPor, anuladaEn, motivoAnulacion);
    }

    /**
     * No toca el stock: anular una solicitud solo la excluye del dataset de demanda. El motivo es
     * obligatorio, igual que en la venta (D-44).
     */
    public void anular(UUID usuarioId, Instant ahora, String motivo) {
        if (estado == EstadoSolicitud.ANULADA) {
            throw new SolicitudYaAnuladaException(id);
        }
        if (usuarioId == null) {
            throw new SolicitudInvalidaException("El usuario que anula es obligatorio");
        }
        this.motivoAnulacion = validarMotivo(motivo);
        this.estado = EstadoSolicitud.ANULADA;
        this.anuladaPor = usuarioId;
        this.anuladaEn = ahora;
    }

    public static String validarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new SolicitudInvalidaException("El motivo de anulación es obligatorio");
        }
        String normalizado = motivo.trim();
        if (normalizado.length() > LONGITUD_MAXIMA_MOTIVO) {
            throw new SolicitudInvalidaException(
                    "El motivo de anulación no puede superar " + LONGITUD_MAXIMA_MOTIVO + " caracteres");
        }
        return normalizado;
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
    public String getMotivoAnulacion() { return motivoAnulacion; }
}
