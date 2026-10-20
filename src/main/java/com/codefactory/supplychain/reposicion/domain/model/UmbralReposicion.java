package com.codefactory.supplychain.reposicion.domain.model;

import com.codefactory.supplychain.reposicion.domain.exception.UmbralInvalidoException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Umbral de reposición interna (Bodega_Tienda -> Almacén): cuándo reponer y hasta
 * dónde llenar el Almacén.
 * <p>
 * El alcance es de <b>una variante</b> o de <b>una categoría</b>, nunca de ambas ni de
 * ninguna (D-51). El alcance no cambia: para pasar de variante a categoría se elimina
 * el umbral y se crea otro. Lo único modificable son las cantidades.
 * <p>
 * Dispara cuando {@code a_la_mano < cantidadMinima} y repone hasta
 * {@code cantidadObjetivo} (D-53, D-54).
 * <p>
 * Objeto de dominio puro e inmutable, igual que {@code Categoria}.
 *
 * Módulo: reposicion — Umbrales de reposición interna (FEAT-09).
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class UmbralReposicion {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID varianteId;
    private final UUID categoriaId;
    private final int cantidadMinima;
    private final int cantidadObjetivo;

    private UmbralReposicion(UUID id, UUID varianteId, UUID categoriaId,
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
    public static UmbralReposicion crear(UUID varianteId, UUID categoriaId,
                                         int cantidadMinima, int cantidadObjetivo) {
        return new UmbralReposicion(UUID.randomUUID(), varianteId, categoriaId,
                cantidadMinima, cantidadObjetivo);
    }

    public static UmbralReposicion reconstruir(UUID id, UUID varianteId, UUID categoriaId,
                                               int cantidadMinima, int cantidadObjetivo) {
        return new UmbralReposicion(id, varianteId, categoriaId, cantidadMinima, cantidadObjetivo);
    }

    /**
     * Cambia el par de cantidades de una vez: validarlas por separado impediría subir
     * mínimo y objetivo a la vez sin romper la regla en un paso intermedio.
     */
    public UmbralReposicion cambiarCantidades(int nuevaCantidadMinima, int nuevaCantidadObjetivo) {
        return new UmbralReposicion(id, varianteId, categoriaId,
                nuevaCantidadMinima, nuevaCantidadObjetivo);
    }

    public boolean esPorVariante() {
        return varianteId != null;
    }

    private static void validarAlcance(UUID varianteId, UUID categoriaId) {
        if (varianteId == null && categoriaId == null) {
            throw new UmbralInvalidoException("El umbral debe aplicar a una variante o a una categoría");
        }
        if (varianteId != null && categoriaId != null) {
            throw new UmbralInvalidoException(
                    "El umbral no puede aplicar a una variante y a una categoría al mismo tiempo");
        }
    }

    private static void validarCantidades(int cantidadMinima, int cantidadObjetivo) {
        if (cantidadMinima <= 0) {
            throw new UmbralInvalidoException("La cantidad mínima debe ser mayor que cero");
        }
        if (cantidadObjetivo <= cantidadMinima) {
            throw new UmbralInvalidoException("La cantidad objetivo debe ser mayor que la cantidad mínima");
        }
    }
}
