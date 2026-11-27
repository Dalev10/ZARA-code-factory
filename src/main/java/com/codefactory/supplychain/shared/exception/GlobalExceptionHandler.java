package com.codefactory.supplychain.shared.exception;

import com.codefactory.supplychain.shared.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduce las excepciones transversales (ReglaDeNegocioException, ReglaNoProcesableException,
 * RecursoDuplicadoException, ConflictoDeEstadoException, etc.) y los errores de validación de Bean Validation a respuestas HTTP consistentes, sin que
 * este componente dependa de las excepciones concretas de ningún módulo específico.
 *
 * Componentes transversales de tipo 'exception', compartidos por todos los módulos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaDeNegocio(ReglaDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ReglaNoProcesableException.class)
    public ResponseEntity<ErrorResponse> handleReglaNoProcesable(ReglaNoProcesableException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoDuplicado(RecursoDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ConflictoDeEstadoException.class)
    public ResponseEntity<ErrorResponse> handleConflictoDeEstado(ConflictoDeEstadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    /** Se agotaron los reintentos optimistas (D-24): otra operación modificó el mismo registro. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencia(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Otra operación modificó el mismo registro; intente de nuevo"));
    }

    @ExceptionHandler(RecursoEnUsoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoEnUso(RecursoEnUsoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(NoAutorizadoException.class)
    public ResponseEntity<ErrorResponse> handleNoAutorizado(NoAutorizadoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ProhibidoException.class)
    public ResponseEntity<ErrorResponse> handleProhibido(ProhibidoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridadReferencial(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("No se puede completar la operación: el recurso está siendo "
                        + "referenciado por otro registro del sistema"));
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleUsoInvalidoDeAccesoADatos(InvalidDataAccessApiUsageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Los parámetros de paginación u ordenamiento son inválidos"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("No tiene permisos para realizar esta acción"));
    }

    /** JSON mal formado o con un valor del tipo equivocado; sin esto Spring responde 400 sin cuerpo. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleCuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("El cuerpo de la solicitud no es un JSON válido para esta operación"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacionBeanValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(mensaje));
    }

    /**
     * Lo que ningún manejador anterior previó (H-09). Las excepciones propias de Spring MVC
     * (ruta inexistente, método no permitido, parámetro faltante…) traen su estado y lo
     * conservan; el resto es un error interno: se registra con su traza y el cliente solo
     * recibe un mensaje genérico, sin detalles de la implementación.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleNoPrevisto(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse deSpring) {
            return respuestaPara(deSpring.getStatusCode());
        }
        if (ex instanceof TypeMismatchException) {
            return respuestaPara(HttpStatus.BAD_REQUEST);
        }
        log.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Ocurrió un error interno; intente de nuevo más tarde"));
    }

    private static ResponseEntity<ErrorResponse> respuestaPara(HttpStatusCode estado) {
        // 406: el cliente no acepta JSON, así que cualquier cuerpo fallaría al escribirse.
        if (estado.value() == HttpStatus.NOT_ACCEPTABLE.value()) {
            return ResponseEntity.status(estado).build();
        }
        String mensaje = switch (estado.value()) {
            case 400 -> "La solicitud tiene parámetros faltantes o con un formato inválido";
            case 404 -> "El recurso solicitado no existe";
            case 405 -> "Este recurso no admite el método HTTP usado";
            case 415 -> "El tipo de contenido de la solicitud no es compatible; use application/json";
            default -> "No se pudo procesar la solicitud";
        };
        return ResponseEntity.status(estado).body(new ErrorResponse(mensaje));
    }
}
