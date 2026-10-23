package com.codefactory.supplychain.qa.soporte;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ScenarioScope
public class ContextoPrueba {

    private String token;
    private Response respuesta;
    private final Map<String, String> ids = new HashMap<>();
    private final Map<String, String> textos = new HashMap<>();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Response getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(Response respuesta) {
        this.respuesta = respuesta;
    }

    public String id(String clave) {
        return ids.get(clave);
    }

    public void id(String clave, String valor) {
        ids.put(clave, valor);
    }

    public String texto(String clave) {
        return textos.get(clave);
    }

    public void texto(String clave, String valor) {
        textos.put(clave, valor);
    }

    public void limpiar() {
        token = null;
        respuesta = null;
        ids.clear();
        textos.clear();
    }
}
