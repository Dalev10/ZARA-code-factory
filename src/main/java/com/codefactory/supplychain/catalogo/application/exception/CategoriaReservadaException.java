package com.codefactory.supplychain.catalogo.application.exception;

import com.codefactory.supplychain.catalogo.domain.model.Categoria;
import com.codefactory.supplychain.shared.exception.ConflictoDeEstadoException;

/**
 * {@value Categoria#NOMBRE_RESERVADO} agrupa los templates que no tenían categoría (D-52):
 * no se crea, renombra ni elimina por API.
 *
 * Módulo: catalogo — Categoria/Template/Variante (FEAT-05).
 */
public class CategoriaReservadaException extends ConflictoDeEstadoException {

    public CategoriaReservadaException() {
        super("La categoría " + Categoria.NOMBRE_RESERVADO + " es reservada: no se crea, renombra ni elimina");
    }
}
