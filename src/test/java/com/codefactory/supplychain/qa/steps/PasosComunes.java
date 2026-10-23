package com.codefactory.supplychain.qa.steps;

import com.codefactory.supplychain.qa.config.ClienteApi;
import com.codefactory.supplychain.qa.soporte.ContextoPrueba;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

public class PasosComunes {

    @Autowired
    private ClienteApi api;

    @Autowired
    private ContextoPrueba contexto;

    @Dado("que estoy autenticado como administrador")
    public void autenticadoComoAdministrador() {
        contexto.setToken(api.iniciarSesionComoAdmin());
    }

    @Dado("que estoy autenticado como {string} con la contraseña {string}")
    public void autenticadoComo(String email, String password) {
        contexto.setToken(api.iniciarSesion(email, password));
    }

    @Cuando("inicio sesión con {string} y la contraseña {string}")
    public void iniciarSesion(String email, String password) {
        Response respuesta = api.postSinAuth("/auth/login", Map.of("email", email, "password", password));
        if (respuesta.statusCode() == 200) {
            contexto.setToken(respuesta.path("accessToken"));
        }
    }

    @Entonces("la respuesta es {int}")
    public void laRespuestaEs(int codigo) {
        contexto.getRespuesta().then().statusCode(codigo);
    }

    @Entonces("la respuesta no es {int}")
    public void laRespuestaNoEs(int codigo) {
        assertThat(contexto.getRespuesta().statusCode()).isNotEqualTo(codigo);
    }

    @Entonces("el campo {string} es {string}")
    public void elCampoEs(String ruta, String valor) {
        contexto.getRespuesta().then().body(ruta, org.hamcrest.Matchers.equalTo(valor));
    }

    @Entonces("el campo {string} es {int}")
    public void elCampoEsEntero(String ruta, int valor) {
        contexto.getRespuesta().then().body(ruta, org.hamcrest.Matchers.equalTo(valor));
    }

    @Entonces("el campo {string} no es nulo")
    public void elCampoNoEsNulo(String ruta) {
        contexto.getRespuesta().then().body(ruta, org.hamcrest.Matchers.notNullValue());
    }

    @Entonces("el campo {string} está vacío")
    public void elCampoEstaVacio(String ruta) {
        contexto.getRespuesta().then().body(ruta, hasSize(0));
    }

    @Entonces("el campo {string} tiene {int} elementos")
    public void elCampoTieneElementos(String ruta, int cantidad) {
        contexto.getRespuesta().then().body(ruta, hasSize(cantidad));
    }

    @Entonces("el mensaje de error no está vacío")
    public void elMensajeDeErrorNoEstaVacio() {
        contexto.getRespuesta().then().body("mensaje", not(emptyOrNullString()));
    }

    @Entonces("el campo {string} termina en Z")
    public void elCampoTerminaEnZ(String ruta) {
        contexto.getRespuesta().then().body(ruta, org.hamcrest.Matchers.endsWith("Z"));
    }
}
