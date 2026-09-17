package com.codefactory.supplychain.inventario.domain.model.bodegatienda;

import com.codefactory.supplychain.inventario.domain.exception.bodegatienda.BodegaTiendaInvalidaException;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;

import java.util.Objects;

/**
 * Entidad de dominio que representa la Bodega de una Tienda física (nodo tipo
 * {@code BODEGA_TIENDA}).
 * 
 * Se comunica directamente con el Centro de Distribución (CD) para
 * solicitudes de reabastecimiento. Abastece al Almacén (piso de venta) de la
 * tienda mediante traspasos internos automáticos basados en umbrales.
 * Esta clase no depende de frameworks ni de persistencia (Arquitectura
 * Hexagonal).
 */
public class BodegaTienda {

    private final Long id;
    private final Long tiendaId;
    private final TipoNodo tipo;

    /**
     * Constructor para la creación de una nueva bodega de tienda (sin persisti aún,
     * id nulo).
     *
     * @param tiendaId Identificador positivo de la tienda física asociada.
     * @throws BodegaTiendaInvalidaException si {@code tiendaId} es nulo o menor o
     *                                       igual a cero.
     */
    public BodegaTienda(Long tiendaId) {
        validarTiendaId(tiendaId);
        this.id = null;
        this.tiendaId = tiendaId;
        this.tipo = TipoNodo.BODEGA_TIENDA;
    }

    /**
     * Constructor para la reconstrucción de una bodega de tienda existente desde
     * persistencia.
     *
     * @param id       Identificador positivo de la entidad/nodo.
     * @param tiendaId Identificador positivo de la tienda física asociada.
     * @throws BodegaTiendaInvalidaException si {@code id} o {@code tiendaId} son
     *                                       nulos o menores o iguales a cero.
     */
    public BodegaTienda(Long id, Long tiendaId) {
        validarId(id);
        validarTiendaId(tiendaId);
        this.id = id;
        this.tiendaId = tiendaId;
        this.tipo = TipoNodo.BODEGA_TIENDA;
    }

    /**
     * Método de fábrica para crear una nueva instancia de {@code BodegaTienda}.
     *
     * @param tiendaId Identificador positivo de la tienda asociada.
     * @return Nueva instancia de {@code BodegaTienda}.
     */
    public static BodegaTienda crear(Long tiendaId) {
        return new BodegaTienda(tiendaId);
    }

    /**
     * Método de fábrica para reconstruir una {@code BodegaTienda} persistida
     * previamente.
     *
     * @param id       Identificador de la bodega de tienda.
     * @param tiendaId Identificador de la tienda asociada.
     * @return Instancia reconstruida de {@code BodegaTienda}.
     */
    public static BodegaTienda reconstruir(Long id, Long tiendaId) {
        return new BodegaTienda(id, tiendaId);
    }

    // MÉTODOS DE VALIDACIÓN

    private static void validarId(Long id) {
        if (id == null) {
            throw new BodegaTiendaInvalidaException("El id de la bodega de tienda no puede ser nulo");
        }
        if (id <= 0) {
            throw new BodegaTiendaInvalidaException("El id de la bodega de tienda debe ser mayor a cero");
        }
    }

    private static void validarTiendaId(Long tiendaId) {
        if (tiendaId == null) {
            throw new BodegaTiendaInvalidaException("El id de la tienda no puede ser nulo");
        }
        if (tiendaId <= 0) {
            throw new BodegaTiendaInvalidaException("El id de la tienda debe ser mayor a cero");
        }
    }

    // GETTERS

    public Long getId() {
        return id;
    }

    public Long getTiendaId() {
        return tiendaId;
    }

    public TipoNodo getTipo() {
        return tipo;
    }

    public boolean tieneId() {
        return this.id != null;
    }

    // IDIOMS

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BodegaTienda that = (BodegaTienda) o;
        if (this.id != null && that.id != null) {
            return Objects.equals(this.id, that.id);
        }
        return Objects.equals(this.tiendaId, that.tiendaId);
    }

    @Override
    public int hashCode() {
        if (this.id != null) {
            return Objects.hash(this.id);
        }
        return Objects.hash(this.tiendaId);
    }

    @Override
    public String toString() {
        return "BodegaTienda{" +
                "id=" + id +
                ", tiendaId=" + tiendaId +
                ", tipo=" + tipo +
                '}';
    }
}
