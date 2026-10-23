package com.codefactory.supplychain.qa.steps;

import com.codefactory.supplychain.qa.config.ClienteApi;
import com.codefactory.supplychain.qa.soporte.ContextoPrueba;
import io.cucumber.java.es.Dado;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Creación de recursos compartidos por todos los módulos. Los pasos guardan el id
 * en {@link ContextoPrueba} con la clave "recurso:alias" y también en "recursoId"
 * para el último recurso creado.
 */
public class PasosRecursos {

    @Autowired
    private ClienteApi api;

    @Autowired
    private ContextoPrueba contexto;

    @Dado("que existe una tienda llamada {string}")
    public void crearTienda(String alias) {
        Response respuesta = api.post("/tiendas", Map.of(
                "nombre", ClienteApi.unico("Tienda " + alias),
                "ubicacion", "Medellín, Antioquia"));
        guardar("tienda", alias, respuesta);
    }

    @Dado("que existe una tienda activa llamada {string}")
    public void crearTiendaActiva(String alias) {
        crearTienda(alias);
    }

    @Dado("que existe un centro de distribución llamado {string}")
    public void crearCentroDistribucion(String alias) {
        Response respuesta = api.post("/centros-distribucion", Map.of(
                "nombre", ClienteApi.unico("CD " + alias),
                "ubicacion", "Envigado, Antioquia"));
        guardar("cd", alias, respuesta);
    }

    @Dado("que existe una categoría llamada {string}")
    public void crearCategoria(String alias) {
        Response respuesta = api.post("/categorias", Map.of(
                "nombre", ClienteApi.unico("Categoría " + alias)));
        guardar("categoria", alias, respuesta);
    }

    @Dado("que existe un template llamado {string} en la categoría {string}")
    public void crearTemplate(String alias, String categoriaAlias) {
        Response respuesta = api.post("/templates", Map.of(
                "nombre", ClienteApi.unico("Template " + alias),
                "temporada", "OI-2026",
                "proveedor", "Proveedor QA",
                "precioBase", 89900,
                "categoriaId", contexto.id("categoria:" + categoriaAlias)));
        guardar("template", alias, respuesta);
    }

    @Dado("que existe una variante para el template {string}")
    public void crearVariante(String templateAlias) {
        Response respuesta = api.post("/variantes", Map.of(
                "sku", ClienteApi.unico("SKU").toUpperCase(),
                "talla", "M",
                "color", "Negro",
                "templateId", contexto.id("template:" + templateAlias)));
        guardar("variante", templateAlias, respuesta);
    }

    private void guardar(String tipo, String alias, Response respuesta) {
        if (respuesta.statusCode() >= 300) {
            throw new IllegalStateException("No se pudo crear " + tipo + " " + alias
                    + ": " + respuesta.statusCode() + " " + respuesta.asString());
        }
        String id = respuesta.path("id");
        contexto.id(tipo + ":" + alias, id);
        contexto.id(tipo + "Id", id);
        contexto.texto(tipo + "Nombre", respuesta.path("nombre"));
    }
}
