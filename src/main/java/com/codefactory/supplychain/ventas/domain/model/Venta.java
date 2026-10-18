package com.codefactory.supplychain.ventas.domain.model;

import com.codefactory.supplychain.ventas.domain.exception.VentaInvalidaException;
import com.codefactory.supplychain.ventas.domain.exception.VentaYaAnuladaException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Línea de venta (una variante dentro de una transacción). Inmutable.
 * Un ticket completo = todas las líneas con el mismo {@code transaccionId} (D-36).
 */
public record Venta(
        UUID id,
        UUID transaccionId,
        UUID varianteId,
        UUID tiendaId,
        int cantidad,
        BigDecimal precio,
        String canal,
        OrigenVenta origen,
        UUID vendedorId,
        boolean forzada,
        EstadoVenta estado,
        Instant timestampVenta,
        UUID anuladaPor,
        Instant anuladaEn,
        String motivoAnulacion) {

    public static final String CANAL_POR_DEFECTO = "FISICO";
    private static final int CANAL_MAX = 30;

    public Venta {
        if (cantidad <= 0) {
            throw new VentaInvalidaException("La cantidad debe ser mayor que cero");
        }
        if (precio == null || precio.signum() < 0) {
            throw new VentaInvalidaException("El precio unitario no puede ser negativo");
        }
        if (canal == null || canal.isBlank() || canal.length() > CANAL_MAX) {
            throw new VentaInvalidaException("El canal es obligatorio y admite hasta " + CANAL_MAX + " caracteres");
        }
        if (origen == OrigenVenta.API && vendedorId == null) {
            throw new VentaInvalidaException("Una venta por API requiere vendedor");
        }
    }

    /** Venta nueva por API: vendedor y hora los fija el servidor (D-10, D-38). */
    public static Venta registrarPorApi(UUID transaccionId, UUID varianteId, UUID tiendaId, int cantidad,
                                        BigDecimal precio, String canal, UUID vendedorId, boolean forzada) {
        return registrarPorApi(transaccionId, varianteId, tiendaId, cantidad, precio, canal, vendedorId, forzada,
                Instant.now());
    }

    public static Venta registrarPorApi(UUID transaccionId, UUID varianteId, UUID tiendaId, int cantidad,
                                        BigDecimal precio, String canal, UUID vendedorId, boolean forzada,
                                        Instant fecha) {
        if (transaccionId == null || varianteId == null || tiendaId == null || vendedorId == null) {
            throw new VentaInvalidaException("Transacción, variante, tienda y vendedor son obligatorios");
        }
        if (fecha == null) {
            throw new VentaInvalidaException("La fecha de venta es obligatoria");
        }
        return new Venta(UUID.randomUUID(), transaccionId, varianteId, tiendaId, cantidad, precio,
                canal == null ? CANAL_POR_DEFECTO : canal.trim(), OrigenVenta.API, vendedorId, forzada,
                EstadoVenta.REGISTRADA, fecha, null, null, null);
    }

    public Venta anular(UUID usuarioId, Instant fecha, String motivo) {
        if (estado == EstadoVenta.ANULADA) {
            throw new VentaYaAnuladaException(id);
        }
        if (usuarioId == null || fecha == null || motivo == null || motivo.isBlank()) {
            throw new VentaInvalidaException("Usuario, fecha y motivo de anulación son obligatorios");
        }
        String motivoNormalizado = motivo.trim();
        if (motivoNormalizado.length() > 500) {
            throw new VentaInvalidaException("El motivo de anulación no puede superar 500 caracteres");
        }
        return new Venta(id, transaccionId, varianteId, tiendaId, cantidad, precio, canal, origen, vendedorId,
                forzada, EstadoVenta.ANULADA, timestampVenta, usuarioId, fecha, motivoNormalizado);
    }

    public boolean coincideCon(UUID transaccion, UUID tienda, UUID variante, int cantidadSolicitada,
                               BigDecimal precioSolicitado, String canalSolicitado, UUID vendedor,
                               boolean ventaForzada) {
        return Objects.equals(transaccionId, transaccion)
                && Objects.equals(tiendaId, tienda)
                && Objects.equals(varianteId, variante)
                && cantidad == cantidadSolicitada
                && precio.compareTo(precioSolicitado) == 0
                && Objects.equals(canal, canalSolicitado)
                && Objects.equals(vendedorId, vendedor)
                && forzada == ventaForzada;
    }
}
