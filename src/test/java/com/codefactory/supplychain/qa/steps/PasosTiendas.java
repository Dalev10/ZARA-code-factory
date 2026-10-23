package com.codefactory.supplychain.qa.steps;

import com.codefactory.supplychain.qa.config.ClienteApi;
import com.codefactory.supplychain.qa.soporte.ContextoPrueba;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class PasosTiendas {

    @Autowired
    private ClienteApi api;

    @Autowired
    private ContextoPrueba contexto;

    @Dado("que ya existe una tienda registrada con nombre único")
    public void tiendaRegistrada() {
        String nombre = ClienteApi.unico("Tienda");
        api.post("/tiendas", Map.of("nombre", nombre, "ubicacion", "Medellín, Antioquia"));
        contexto.texto("tiendaNombre", nombre);
        contexto.id("tiendaId", contexto.getRespuesta().path("id"));
    }

    @Cuando("registro una tienda con nombre único y ubicación {string}")
    public void registrarTienda(String ubicacion) {
        String nombre = ClienteApi.unico("Tienda");
        contexto.texto("tiendaNombre", nombre);
        api.post("/tiendas", Map.of("nombre", nombre, "ubicacion", ubicacion));
        contexto.id("tiendaId", contexto.getRespuesta().path("id"));
    }

    @Cuando("registro una tienda con nombre vacío y ubicación {string}")
    public void registrarTiendaSinNombre(String ubicacion) {
        api.post("/tiendas", Map.of("nombre", "", "ubicacion", ubicacion));
    }

    @Cuando("registro otra tienda con el mismo nombre")
    public void registrarTiendaDuplicada() {
        api.post("/tiendas", Map.of("nombre", contexto.texto("tiendaNombre"), "ubicacion", "Otra ubicación"));
    }

    @Entonces("el campo {string} es el nombre único de la tienda")
    public void elCampoEsElNombreDeLaTienda(String ruta) {
        contexto.getRespuesta().then().body(ruta,
                org.hamcrest.Matchers.equalTo(contexto.texto("tiendaNombre")));
    }
}
