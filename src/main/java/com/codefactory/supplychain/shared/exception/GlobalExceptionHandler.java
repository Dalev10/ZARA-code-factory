package com.codefactory.supplychain.shared.exception;

import com.codefactory.supplychain.shared.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduce las excepciones transversales (ReglaDeNegocioException, RecursoDuplicadoException)
 * y los errores de validación de Bean Validation a respuestas HTTP consistentes, sin que
 * este componente dependa de las excepciones concretas de ningún módulo específico.
 *
 * Componentes transversales de tipo 'exception', compartidos por todos los módulos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaDeNegocio(ReglaDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoDuplicado(RecursoDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(NoAutorizadoException.class)
    public ResponseEntity<ErrorResponse> handleNoAutorizado(NoAutorizadoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("No tenés permisos para realizar esta acción"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacionBeanValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(mensaje));
    }
}
