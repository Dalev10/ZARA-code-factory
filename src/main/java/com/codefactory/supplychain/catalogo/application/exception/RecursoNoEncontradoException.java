package com.codefactory.supplychain.catalogo.application.exception;

/**
 * Se lanza cuando se intenta consultar, modificar, eliminar o referenciar
 * (por ejemplo, al asociar una relación) una entidad del catálogo
 * (Categoria, Template o Variante) que no existe.
 * <p>
 * NOTA: no se encontró en el ZIP de esta etapa una estructura de
 * excepciones compartida en {@code shared/exception} para revisar sus
 * convenciones (el ZIP entregado solo contiene el dominio, los puertos de
 * salida y la persistencia del módulo {@code catalogo}; no incluye el
 * resto del repositorio). Por eso se crea aquí una excepción mínima y
 * específica del módulo, en lugar de usar {@link RuntimeException}
 * genérica. Si el proyecto real ya posee una jerarquía de excepciones en
 * {@code com.codefactory.supplychain.shared.exception}, esta clase debería
 * adaptarse para extenderla (por ejemplo, heredando de una
 * {@code ApplicationException} o {@code NotFoundException} común) en lugar
 * de extender {@link RuntimeException} directamente.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException categoria(Long id) {
        return new RecursoNoEncontradoException("No existe una Categoria con id " + id);
    }

    public static RecursoNoEncontradoException template(Long id) {
        return new RecursoNoEncontradoException("No existe un Template con id " + id);
    }

    public static RecursoNoEncontradoException variante(Long id) {
        return new RecursoNoEncontradoException("No existe una Variante con id " + id);
    }

    public static RecursoNoEncontradoException varianteConSku(String sku) {
        return new RecursoNoEncontradoException("No existe una Variante con sku " + sku);
    }
}
