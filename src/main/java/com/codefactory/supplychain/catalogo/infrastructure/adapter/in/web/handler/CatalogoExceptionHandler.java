package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.handler;

import com.codefactory.supplychain.catalogo.application.exception.DatosInvalidosException;
import com.codefactory.supplychain.catalogo.application.exception.RecursoNoEncontradoException;
import com.codefactory.supplychain.catalogo.application.exception.SkuDuplicadoException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador centralizado de excepciones para la capa REST del módulo
 * catalogo (FEAT-05 / HU-15 a HU-18).
 * <p>
 * Traduce las excepciones de la capa de aplicación (y los errores de
 * validación de Bean Validation) a respuestas {@code application/problem+json}
 * consistentes, sin exponer detalles internos del servidor (stack
 * traces, etc.).
 */
@RestControllerAdvice
public class CatalogoExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Recurso no encontrado");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(SkuDuplicadoException.class)
    public ResponseEntity<ProblemDetail> manejarSkuDuplicado(SkuDuplicadoException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("SKU duplicado");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<ProblemDetail> manejarDatosInvalidos(DatosInvalidosException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Datos inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Errores de validación de {@code @Valid} sobre los Request DTO
     * (Jakarta Validation). Se sobrescribe el manejador que ya provee
     * {@link ResponseEntityExceptionHandler} para devolver un
     * {@link ProblemDetail} con el detalle de los campos inválidos, en
     * lugar del cuerpo por defecto.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                    HttpHeaders headers,
                                                                    HttpStatusCode status,
                                                                    WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage()));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos");
        problemDetail.setTitle("Datos inválidos");
        problemDetail.setProperty("errores", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
}
