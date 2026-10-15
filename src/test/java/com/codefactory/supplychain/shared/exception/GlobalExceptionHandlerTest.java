package com.codefactory.supplychain.shared.exception;

import com.codefactory.supplychain.shared.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
}
