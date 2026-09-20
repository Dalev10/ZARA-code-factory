package com.codefactory.supplychain.catalogo.application.exception;

import com.codefactory.supplychain.shared.exception.ReglaDeNegocioException;

/**
 * Se lanza cuando los datos recibidos por un caso de uso violan una
 * invariante mínima que debe protegerse en la capa de aplicación (por
 * ejemplo, un nombre de Categoria vacío o null).
 *
 * Módulo: catalogo — Categoria/Template/Variante (FEAT-05).
 */
public class DatosInvalidosException extends ReglaDeNegocioException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
