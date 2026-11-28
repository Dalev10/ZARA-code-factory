package com.codefactory.supplychain.shared.exception;

import com.codefactory.supplychain.shared.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unaProhibidoExceptionDeCualquierModuloSeTraduceA403ConSuMensaje() {
        ProhibidoException excepcion = new ProhibidoException("No tiene asignada esta tienda") {
        };

        ResponseEntity<ErrorResponse> respuesta = handler.handleProhibido(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).isEqualTo(new ErrorResponse("No tiene asignada esta tienda"));
    }

    @Test
    void unaReglaNoProcesableExceptionDeCualquierModuloSeTraduceA422ConSuMensaje() {
        ReglaNoProcesableException excepcion = new ReglaNoProcesableException("Se recibió más de lo despachado") {
        };

        ResponseEntity<ErrorResponse> respuesta = handler.handleReglaNoProcesable(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(respuesta.getBody()).isEqualTo(new ErrorResponse("Se recibió más de lo despachado"));
    }

    @Test
    void unaRecursoEnUsoExceptionSeTraduceA409ConSuMensaje() {
        RecursoEnUsoException excepcion = new RecursoEnUsoException("El CD tiene prioridades asignadas") {
        };

        ResponseEntity<ErrorResponse> respuesta = handler.handleRecursoEnUso(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody()).isEqualTo(new ErrorResponse("El CD tiene prioridades asignadas"));
    }

    @Test
    void unErrorNoPrevistoResponde500SinRevelarElDetalleInterno() {
        RuntimeException excepcion = new IllegalStateException("could not execute statement [insert into venta ...]");

        ResponseEntity<ErrorResponse> respuesta = handler.handleNoPrevisto(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody()).isEqualTo(new ErrorResponse("Ocurrió un error interno; intente de nuevo más tarde"));
    }

    @Test
    void unaExcepcionDeSpringMvcConservaSuEstadoConCuerpoErrorResponse() {
        ResponseEntity<ErrorResponse> respuesta =
                handler.handleNoPrevisto(new HttpRequestMethodNotSupportedException("DELETE"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(respuesta.getBody()).isEqualTo(new ErrorResponse("Este recurso no admite el método HTTP usado"));
    }

    @Test
    void unParametroConTipoEquivocadoResponde400() {
        ResponseEntity<ErrorResponse> respuesta =
                handler.handleNoPrevisto(new TypeMismatchException("no-es-uuid", UUID.class));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
