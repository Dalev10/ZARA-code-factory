package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.TraspasoInvalidoException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Traspaso de unidades de la Bodega_Tienda al Almacén de la misma tienda.
 * <p>
 * <b>Regla de negocio central:</b>
 * <pre>
 *   cantidad = MIN(objetivo − MAX(almacén, 0), bodega)
 * </pre>
 * <ul>
 *   <li>{@code MAX(almacén, 0)} evita que un Almacén en negativo (venta forzada)
 *       infle la cantidad; llevamos el stock al objetivo, no más allá.</li>
 *   <li>{@code MIN(…, bodega)} nunca deja la Bodega en negativo.</li>
 *   <li>Si el resultado es ≤ 0 no hay traspaso; el llamante no debe crear el objeto.</li>
 * </ul>
 *
 * <b>Condición de disparo:</b> {@code almacén < cantidadMinima}.
 * <p>
 * El traspaso AUTOMATICO no tiene {@code registradoPor}; el MANUAL lo requiere.
 *
 * Módulo: reposicion — Reposición interna.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class TraspasoInterno {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID varianteId;
    private final UUID tiendaId;
    private final int cantidad;
    private final OrigenTraspaso origen;
    /** Siempre null cuando origen = AUTOMATICO; nunca null cuando origen = MANUAL. */
    private final UUID registradoPor;
    private final Instant timestampTraspaso;

    private TraspasoInterno(UUID id, UUID varianteId, UUID tiendaId, int cantidad,
                            OrigenTraspaso origen, UUID registradoPor, Instant timestampTraspaso) {
        this.id = id;
        this.varianteId = varianteId;
        this.tiendaId = tiendaId;
        this.cantidad = cantidad;
        this.origen = origen;
        this.registradoPor = registradoPor;
        this.timestampTraspaso = timestampTraspaso;
        validar(cantidad, origen, registradoPor);
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    /**
     * Calcula la cantidad a traspasar según D-54 y crea el objeto si hay algo que
     * mover. Devuelve {@code null} cuando la cantidad calculada es ≤ 0, es decir,
     * cuando el Almacén ya está en o por encima del objetivo o la Bodega está vacía.
     *
     * @param almacenActual     stock actual del Almacén (puede ser negativo por venta forzada)
     * @param bodegaActual      stock actual de la Bodega_Tienda (≥ 0 por invariante)
     * @param cantidadObjetivo  nivel al que se quiere llevar el Almacén
     */
    public static TraspasoInterno calcularAutomatico(UUID varianteId, UUID tiendaId,
                                                     int almacenActual, int bodegaActual,
                                                     int cantidadObjetivo) {
        int cantidad = calcularCantidad(almacenActual, bodegaActual, cantidadObjetivo);
        if (cantidad <= 0) {
            return null;
        }
        return new TraspasoInterno(UUID.randomUUID(), varianteId, tiendaId, cantidad,
                OrigenTraspaso.AUTOMATICO, null, Instant.now());
    }

    /**
     * Reconstruye un traspaso desde la persistencia (todos los campos conocidos).
     */
    public static TraspasoInterno reconstruir(UUID id, UUID varianteId, UUID tiendaId,
                                              int cantidad, OrigenTraspaso origen,
                                              UUID registradoPor, Instant timestampTraspaso) {
        return new TraspasoInterno(id, varianteId, tiendaId, cantidad, origen,
                registradoPor, timestampTraspaso);
    }

    // -------------------------------------------------------------------------
    // Lógica pura (estática, sin efectos secundarios)
    // -------------------------------------------------------------------------

    /**
     * Implementación de D-54:
     * <pre>MIN(objetivo − MAX(almacén, 0), bodega)</pre>
     * Puede devolver 0 o negativo; el llamante decide si crear o no el traspaso.
     */
    public static int calcularCantidad(int almacenActual, int bodegaActual, int cantidadObjetivo) {
        int deficit = cantidadObjetivo - Math.max(almacenActual, 0);
        return Math.min(deficit, bodegaActual);
    }

    // -------------------------------------------------------------------------
    // Validaciones
    // -------------------------------------------------------------------------

    private static void validar(int cantidad, OrigenTraspaso origen, UUID registradoPor) {
        if (cantidad <= 0) {
            throw new TraspasoInvalidoException("La cantidad del traspaso debe ser mayor que cero");
        }
        if (origen == OrigenTraspaso.MANUAL && registradoPor == null) {
            throw new TraspasoInvalidoException(
                    "El traspaso manual requiere indicar quién lo registró");
        }
        if (origen == OrigenTraspaso.AUTOMATICO && registradoPor != null) {
            throw new TraspasoInvalidoException(
                    "El traspaso automático no debe tener registrador");
        }
    }
}
