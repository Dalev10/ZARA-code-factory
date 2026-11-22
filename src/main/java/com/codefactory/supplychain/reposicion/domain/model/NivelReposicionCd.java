package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.NivelReposicionInvalidoException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Nivel de reposición al CD (HD-04, D-74): cuándo pedir mercancía al CD para una Bodega_Tienda
 * y hasta dónde llenarla. Es distinto del umbral Bodega → Almacén ({@link UmbralReposicion}).
 * <p>
 * El alcance es de <b>una variante</b> o de <b>una categoría</b>, nunca de ambas ni de ninguna.
 * El alcance no cambia: para pasar de variante a categoría se elimina el nivel y se crea otro.
 * Lo único modificable son las cantidades.
 * <p>
 * Se recomienda cuando el {@code pronosticado} de la Bodega queda bajo {@code cantidadMinima},
 * por {@code cantidadObjetivo − pronosticado} (D-75, D-76).
 *
 * Módulo: reposicion — Abastecimiento desde el CD (Sprint 3).
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class NivelReposicionCd {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID varianteId;
    private final UUID categoriaId;
    private final int cantidadMinima;
    private final int cantidadObjetivo;

    private NivelReposicionCd(UUID id, UUID varianteId, UUID categoriaId,
                              int cantidadMinima, int cantidadObjetivo) {
        this.id = id;
        this.varianteId = varianteId;
        this.categoriaId = categoriaId;
        this.cantidadMinima = cantidadMinima;
        this.cantidadObjetivo = cantidadObjetivo;
        validarAlcance(varianteId, categoriaId);
        validarCantidades(cantidadMinima, cantidadObjetivo);
    }

    /**
     * @param varianteId  variante a la que aplica, o {@code null} si es por categoría.
     * @param categoriaId categoría a la que aplica, o {@code null} si es por variante.
     */
    public static NivelReposicionCd crear(UUID varianteId, UUID categoriaId,
                                          int cantidadMinima, int cantidadObjetivo) {
        return new NivelReposicionCd(UUID.randomUUID(), varianteId, categoriaId, cantidadMinima, cantidadObjetivo);
    }

    public static NivelReposicionCd reconstruir(UUID id, UUID varianteId, UUID categoriaId,
                                                int cantidadMinima, int cantidadObjetivo) {
        return new NivelReposicionCd(id, varianteId, categoriaId, cantidadMinima, cantidadObjetivo);
    }

    /** Cambia el par de cantidades de una vez, para no romper la regla en un paso intermedio. */
    public NivelReposicionCd cambiarCantidades(int nuevaCantidadMinima, int nuevaCantidadObjetivo) {
        return new NivelReposicionCd(id, varianteId, categoriaId, nuevaCantidadMinima, nuevaCantidadObjetivo);
    }

    public boolean esPorVariante() {
        return varianteId != null;
    }

    private static void validarAlcance(UUID varianteId, UUID categoriaId) {
        if (varianteId == null && categoriaId == null) {
            throw new NivelReposicionInvalidoException(
                    "El nivel de reposición debe aplicar a una variante o a una categoría");
        }
        if (varianteId != null && categoriaId != null) {
            throw new NivelReposicionInvalidoException(
                    "El nivel de reposición no puede aplicar a una variante y a una categoría al mismo tiempo");
        }
    }

    private static void validarCantidades(int cantidadMinima, int cantidadObjetivo) {
        if (cantidadMinima <= 0) {
            throw new NivelReposicionInvalidoException("La cantidad mínima debe ser mayor que cero");
        }
        if (cantidadObjetivo <= cantidadMinima) {
            throw new NivelReposicionInvalidoException("La cantidad objetivo debe ser mayor que la cantidad mínima");
        }
    }
}
