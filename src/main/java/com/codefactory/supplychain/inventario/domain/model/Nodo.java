package com.codefactory.supplychain.inventario.domain.model;

import com.codefactory.supplychain.inventario.domain.exception.NodoInvalidoException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Nodo polimórfico de la red de distribución: representa exactamente un
 * Centro de Distribución, o la Bodega/Almacén de una Tienda específica.
 * Es la abstracción compartida que permite a {@code Inventario} apuntar de
 * forma genérica a cualquiera de los tres sin conocer de cuál se trata.
 * <p>
 * CentroDistribucion y Tienda son agregados propios (con su propia tabla)
 * que aprovisionan un Nodo satélite al crearse. BodegaTienda y Almacén NO
 * tienen tabla propia — son, literalmente, un Nodo de ese tipo; sus reglas
 * de negocio viven en el servicio de aplicación correspondiente, no acá.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Nodo {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final TipoNodo tipo;
    private final UUID cdId;
    private final UUID tiendaId;

    private Nodo(UUID id, TipoNodo tipo, UUID cdId, UUID tiendaId) {
        validarReferenciaCoherente(tipo, cdId, tiendaId);
        this.id = id;
        this.tipo = tipo;
        this.cdId = cdId;
        this.tiendaId = tiendaId;
    }

    public static Nodo crearParaCd(UUID cdId) {
        return new Nodo(UUID.randomUUID(), TipoNodo.CD, cdId, null);
    }

    /**
     * @param tipo {@link TipoNodo#BODEGA_TIENDA} o {@link TipoNodo#ALMACEN} — nunca {@code CD}.
     */
    public static Nodo crearParaTienda(UUID tiendaId, TipoNodo tipo) {
        if (tipo == TipoNodo.CD) {
            throw new NodoInvalidoException("Un nodo asociado a una tienda no puede ser de tipo CD");
        }
        return new Nodo(UUID.randomUUID(), tipo, null, tiendaId);
    }

    public static Nodo reconstruir(UUID id, TipoNodo tipo, UUID cdId, UUID tiendaId) {
        return new Nodo(id, tipo, cdId, tiendaId);
    }

    private static void validarReferenciaCoherente(TipoNodo tipo, UUID cdId, UUID tiendaId) {
        if (tipo == null) {
            throw new NodoInvalidoException("El tipo de nodo no puede ser nulo");
        }
        boolean esCd = tipo == TipoNodo.CD;
        boolean referenciaValida = esCd
                ? (cdId != null && tiendaId == null)
                : (tiendaId != null && cdId == null);
        if (!referenciaValida) {
            throw new NodoInvalidoException(
                    "Un nodo de tipo " + tipo + " debe referenciar exactamente a "
                            + (esCd ? "un Centro de Distribución" : "una Tienda"));
        }
    }
}
