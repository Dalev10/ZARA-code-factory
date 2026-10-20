package com.codefactory.supplychain.inventario.domain.model;

import com.codefactory.supplychain.inventario.domain.exception.AjusteDejaStockNegativoException;
import com.codefactory.supplychain.inventario.domain.exception.InventarioInvalidoException;
import com.codefactory.supplychain.inventario.domain.exception.StockInsuficienteException;

import java.time.Instant;
import java.util.UUID;

/**
 * Agregado que mantiene las magnitudes y reglas de un inventario.
 *
 * {@code disponible} y {@code pronosticado} son magnitudes derivadas y solo
 * se actualizan en {@link #recalcular()}.
 */
public final class Inventario {

    private final UUID id;
    private final UUID nodoId;
    private final UUID varianteId;
    private final int version;
    private int aLaMano;
    private int entrante;
    private int saliente;
    private int disponible;
    private int pronosticado;

    private Inventario(
            UUID id,
            UUID nodoId,
            UUID varianteId,
            int aLaMano,
            int entrante,
            int saliente,
            int version) {
        if (id == null || nodoId == null || varianteId == null) {
            throw new InventarioInvalidoException("El inventario debe tener id, nodo y variante");
        }
        if (entrante < 0 || saliente < 0) {
            throw new InventarioInvalidoException("Entrante y saliente no pueden ser negativos");
        }
        this.id = id;
        this.nodoId = nodoId;
        this.varianteId = varianteId;
        this.version = version;
        this.aLaMano = aLaMano;
        this.entrante = entrante;
        this.saliente = saliente;
        recalcular();
    }

    public static Inventario crear(UUID nodoId, UUID varianteId) {
        return new Inventario(UUID.randomUUID(), nodoId, varianteId, 0, 0, 0, 0);
    }

    public static Inventario reconstruir(
            UUID id,
            UUID nodoId,
            UUID varianteId,
            int aLaMano,
            int entrante,
            int saliente) {
        return new Inventario(id, nodoId, varianteId, aLaMano, entrante, saliente, 0);
    }

    public static Inventario reconstruir(
            UUID id,
            UUID nodoId,
            UUID varianteId,
            int aLaMano,
            int entrante,
            int saliente,
            int version) {
        return new Inventario(id, nodoId, varianteId, aLaMano, entrante, saliente, version);
    }

    /**
     * Una carga en 0 es válida y deja la variante cargada (D-30).
     *
     * @param usuarioId autor de la carga, tomado del token
     */
    public MovimientoInventario cargar(int cantidad, UUID usuarioId) {
        if (cantidad < 0) {
            throw new InventarioInvalidoException("La cantidad cargada no puede ser negativa");
        }
        aLaMano += cantidad;
        return registrar(TipoMovimientoInventario.CARGA_INICIAL, cantidad, null, null, false, usuarioId, null);
    }

    /**
     * Sin {@code forzado}, rechaza la venta si supera {@code aLaMano}. Forzada,
     * puede dejar el saldo negativo (D-35).
     *
     * @param ventaId venta que origina el movimiento (referencia del kardex)
     */
    public MovimientoInventario aplicarVenta(int cantidad, boolean forzado, UUID ventaId) {
        validarCantidadPositiva(cantidad);
        if (!forzado && cantidad > aLaMano) {
            throw new StockInsuficienteException(aLaMano, cantidad);
        }
        aLaMano -= cantidad;
        return registrar(TipoMovimientoInventario.VENTA, -cantidad, null, null, forzado, null, ventaId);
    }

    /** Salida de un traspaso interno: nunca deja el nodo de origen en negativo (D-56). */
    public MovimientoInventario despacharTraspaso(int cantidad, UUID traspasoId) {
        validarCantidadPositiva(cantidad);
        if (cantidad > aLaMano) {
            throw new StockInsuficienteException(aLaMano, cantidad);
        }
        aLaMano -= cantidad;
        return registrar(TipoMovimientoInventario.TRASPASO_SALIDA, -cantidad, null, null, false, null, traspasoId);
    }

    public MovimientoInventario recibirTraspaso(int cantidad, UUID traspasoId) {
        validarCantidadPositiva(cantidad);
        aLaMano += cantidad;
        return registrar(TipoMovimientoInventario.TRASPASO_ENTRADA, cantidad, null, null, false, null, traspasoId);
    }

    /**
     * Corrige la fila con un motivo cerrado (D-29). A diferencia de la venta
     * forzada, nunca deja {@code aLaMano} en negativo.
     *
     * @param comentario obligatorio cuando el motivo es CORRECCION
     * @param usuarioId  autor del ajuste, tomado del token
     */
    public MovimientoInventario aplicarAjuste(
            int delta,
            MotivoAjuste motivo,
            String comentario,
            UUID usuarioId) {
        if (motivo == null) {
            throw new InventarioInvalidoException("El ajuste debe tener un motivo");
        }
        if (motivo == MotivoAjuste.CORRECCION && (comentario == null || comentario.isBlank())) {
            throw new InventarioInvalidoException("Un ajuste por CORRECCION exige comentario");
        }
        if (delta == 0) {
            throw new InventarioInvalidoException("El ajuste debe modificar el inventario");
        }
        if (aLaMano + delta < 0) {
            throw new AjusteDejaStockNegativoException(aLaMano, delta);
        }
        aLaMano += delta;
        return registrar(TipoMovimientoInventario.AJUSTE, delta, motivo.name(), comentario, false, usuarioId, null);
    }

    /** Devuelve al nodo las unidades de una venta anulada. */
    public MovimientoInventario revertir(int cantidad, UUID ventaId) {
        validarCantidadPositiva(cantidad);
        aLaMano += cantidad;
        return registrar(TipoMovimientoInventario.ANULACION_VENTA, cantidad, null, null, false, null, ventaId);
    }

    private MovimientoInventario registrar(
            TipoMovimientoInventario tipo,
            int cantidad,
            String motivo,
            String comentario,
            boolean forzado,
            UUID usuarioId,
            UUID referenciaId) {
        recalcular();
        return new MovimientoInventario(
                UUID.randomUUID(),
                tipo,
                cantidad,
                motivo,
                comentario,
                forzado,
                usuarioId,
                referenciaId,
                aLaMano,
                Instant.now());
    }

    private void recalcular() {
        disponible = aLaMano;
        pronosticado = aLaMano + entrante - saliente;
    }

    private static void validarCantidadPositiva(int cantidad) {
        if (cantidad <= 0) {
            throw new InventarioInvalidoException("La cantidad debe ser mayor que cero");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getNodoId() {
        return nodoId;
    }

    public UUID getVarianteId() {
        return varianteId;
    }

    public int getVersion() {
        return version;
    }

    public int getALaMano() {
        return aLaMano;
    }

    public int getEntrante() {
        return entrante;
    }

    public int getSaliente() {
        return saliente;
    }

    public int getDisponible() {
        return disponible;
    }

    public int getPronosticado() {
        return pronosticado;
    }
}
