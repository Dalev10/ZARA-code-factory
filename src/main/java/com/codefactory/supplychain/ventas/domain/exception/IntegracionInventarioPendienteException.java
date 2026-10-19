package com.codefactory.supplychain.ventas.domain.exception;

/**
 * El contrato de Inventario que necesita este caso de uso todavía no tiene implementación.
 * Se traduce a HTTP 503 (ver VentasExceptionHandler). Ver MATRIZ_INTEGRACION_INVENTARIO.md.
 */
public class IntegracionInventarioPendienteException extends RuntimeException {

    public IntegracionInventarioPendienteException(String contrato) {
        super("Integración con Inventario pendiente: no hay implementación de " + contrato);
    }
}
