package com.codefactory.supplychain.catalogo.application.exception;

/**
 * Se lanza cuando los datos recibidos por un caso de uso violan una
 * invariante mínima que debe protegerse en la capa de aplicación (por
 * ejemplo, un nombre de Categoria vacío o null), independientemente de
 * las validaciones de entrada (Bean Validation) que se agregarán más
 * adelante en la capa REST/DTO.
 * <p>
 * Ver la nota sobre convenciones de excepciones en
 * {@link RecursoNoEncontradoException}.
 */
public class DatosInvalidosException extends RuntimeException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
