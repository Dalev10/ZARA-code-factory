package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.RecomendacionInvalidaException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Recomendación de reponer una variante en la Bodega_Tienda desde un CD.
 * <p>
 * <b>Regla (D-75, D-76):</b> si {@code pronosticado < cantidadMinima}, entonces
 * <pre>
 *   cantidad_sugerida = cantidad_objetivo − pronosticado
 * </pre>
 * y solo se recomienda si es mayor que 0.
 * <p>
 * El CD sugerido es el primero de la prioridad de la tienda, sin validar stock;
 * nulo si la tienda no tiene CDs. Nace en BORRADOR.
 *
 * Módulo: reposicion — HU-43.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Recomendacion {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID tiendaId;
    private final UUID varianteId;
    private final UUID cdSugeridoId;
    private final int cantidadSugerida;
    private final EstadoRecomendacion estado;
    private final Instant fechaCreacion;

    private Recomendacion(UUID id, UUID tiendaId, UUID varianteId, UUID cdSugeridoId,
                          int cantidadSugerida, EstadoRecomendacion estado, Instant fechaCreacion) {
        this.id = Objects.requireNonNull(id, "id");
        this.tiendaId = Objects.requireNonNull(tiendaId, "tiendaId");
        this.varianteId = Objects.requireNonNull(varianteId, "varianteId");
        this.cdSugeridoId = cdSugeridoId;
        this.cantidadSugerida = cantidadSugerida;
        this.estado = Objects.requireNonNull(estado, "estado");
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion, "fechaCreacion");
        if (cantidadSugerida <= 0) {
            throw new RecomendacionInvalidaException("La cantidad sugerida debe ser mayor que cero");
        }
    }

    /**
     * Aplica la regla de HU-43. Vacío si el pronosticado no está bajo el mínimo o si
     * la cantidad sugerida no es mayor que 0.
     */
    public static Optional<Recomendacion> evaluar(UUID tiendaId, UUID varianteId, int pronosticado,
                                                  int cantidadMinima, int cantidadObjetivo,
                                                  UUID cdSugeridoId, Instant ahora) {
        if (!dispara(pronosticado, cantidadMinima)) {
            return Optional.empty();
        }
        int cantidad = calcularCantidadSugerida(pronosticado, cantidadObjetivo);
        if (cantidad <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Recomendacion(UUID.randomUUID(), tiendaId, varianteId, cdSugeridoId,
                cantidad, EstadoRecomendacion.BORRADOR, ahora));
    }

    public static Recomendacion reconstruir(UUID id, UUID tiendaId, UUID varianteId, UUID cdSugeridoId,
                                            int cantidadSugerida, EstadoRecomendacion estado,
                                            Instant fechaCreacion) {
        return new Recomendacion(id, tiendaId, varianteId, cdSugeridoId, cantidadSugerida, estado, fechaCreacion);
    }

    /** D-75: la recomendación se dispara cuando el pronosticado queda bajo el mínimo. */
    public static boolean dispara(int pronosticado, int cantidadMinima) {
        return pronosticado < cantidadMinima;
    }

    /** D-76: objetivo − pronosticado. Puede ser ≤ 0; el llamante decide. */
    public static int calcularCantidadSugerida(int pronosticado, int cantidadObjetivo) {
        return cantidadObjetivo - pronosticado;
    }
}
