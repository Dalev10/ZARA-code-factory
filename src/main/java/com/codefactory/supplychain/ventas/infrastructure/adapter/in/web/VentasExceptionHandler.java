package com.codefactory.supplychain.ventas.infrastructure.adapter.in.web;

import com.codefactory.supplychain.ventas.domain.exception.IntegracionInventarioPendienteException;
import com.codefactory.supplychain.shared.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce la única excepción propia de ventas que el GlobalExceptionHandler no conoce (las demás
 * extienden las bases de shared/exception). Limitado a los controladores de este módulo.
 */
@RestControllerAdvice(basePackageClasses = VentasExceptionHandler.class)
public class VentasExceptionHandler {

    @ExceptionHandler(IntegracionInventarioPendienteException.class)
    public ResponseEntity<ErrorResponse> handleIntegracionPendiente(IntegracionInventarioPendienteException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse(ex.getMessage()));
    }
}
