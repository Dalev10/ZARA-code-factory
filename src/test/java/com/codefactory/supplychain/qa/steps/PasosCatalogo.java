package com.codefactory.supplychain.qa.steps;

import com.codefactory.supplychain.qa.config.ClienteApi;
import com.codefactory.supplychain.qa.soporte.CatalogoDatos;
import com.codefactory.supplychain.qa.soporte.ContextoPrueba;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;

public class PasosCatalogo {

    @Autowired
    private ClienteApi api;

    @Autowired
    private ContextoPrueba contexto;

    @Dado("que existe un producto registrado con SKU {string}")
    public void existeProducto(String sku) {
        contexto.texto("productoSku", sku);
    }

    @Dado("que no existe un producto con SKU {string}")
    public void noExisteProducto(String sku) {
        contexto.texto("productoSku", sku);
    }

    @Dado("que el producto tiene inventario y operaciones asociadas")
    public void productoTieneDependencias() {
        contexto.texto("productoConDependencias", "true");
    }

    @Cuando("registro un producto con SKU {string}, nombre {string} y situación {string}")
    public void registrarProducto(String sku, String nombre, String situacion) {
        contexto.texto("productoSku", sku);
        contexto.texto("productoNombre", nombre);
        contexto.texto("productoSituacion", situacion);
    }

    @Cuando("registro un producto sin nombre ni SKU")
    public void registrarProductoSinDatosObligatorios() {
        contexto.texto("productoIncompleto", "true");
    }

    @Cuando("registro otro producto con el mismo SKU {string}")
    public void registrarProductoDuplicado(String sku) {
        contexto.texto("productoSkuDuplicado", sku);
    }

    @Cuando("registro un producto con SKU {string} y lo asocio a la tienda {string}")
    public void registrarProductoEnTienda(String sku, String tiendaAlias) {
        contexto.texto("productoSku", sku);
        contexto.texto("productoTienda", tiendaAlias);
    }

    @Cuando("consulto el producto con SKU {string}")
    public void consultarProducto(String sku) {
        contexto.texto("productoSku", sku);
    }

    @Cuando("busco productos por el criterio {string}")
    public void buscarProductos(String criterio) {
        contexto.texto("productoCriterio", criterio);
    }

    @Cuando("consulto los productos de la tienda {string} según mis responsabilidades")
    public void consultarProductosSegunResponsabilidades(String tiendaAlias) {
        contexto.texto("productoTienda", tiendaAlias);
    }

    @Cuando("consulto la información logística del producto con SKU {string}")
    public void consultarInformacionLogistica(String sku) {
        contexto.texto("productoSku", sku);
    }

    @Cuando("modifico el producto con SKU {string} con el nombre {string}")
    public void modificarNombreProducto(String sku, String nombre) {
        contexto.texto("productoSku", sku);
        contexto.texto("productoNombre", nombre);
    }

    @Cuando("modifico el producto con SKU {string} con un precio inválido")
    public void modificarProductoConPrecioInvalido(String sku) {
        contexto.texto("productoSku", sku);
        contexto.texto("productoPrecioInvalido", "true");
    }

    @Cuando("modifico el producto con SKU {string} y consulto el inventario de la tienda {string}")
    public void modificarProductoYConsultarInventario(String sku, String tiendaAlias) {
        contexto.texto("productoSku", sku);
        contexto.texto("productoTienda", tiendaAlias);
    }

    @Cuando("elimino el producto con SKU {string}")
    public void eliminarProducto(String sku) {
        contexto.texto("productoSku", sku);
    }

    @Cuando("intento eliminar el producto con SKU {string}")
    public void intentarEliminarProducto(String sku) {
        contexto.texto("productoSku", sku);
    }

    @Cuando("consulto el historial del producto con SKU {string}")
    public void consultarHistorialProducto(String sku) {
        contexto.texto("productoSku", sku);
    }

    @Cuando("registro un template llamado {string} sin categoría")
    public void registrarTemplateSinCategoria(String nombre) {
        api.post("/templates", CatalogoDatos.templateSinCategoria(ClienteApi.unico(nombre)));
    }

    @Cuando("registro un template llamado {string} en la categoría {string}")
    public void registrarTemplateConCategoria(String nombre, String categoriaAlias) {
        Response respuesta = api.post("/templates", CatalogoDatos.templateConCategoria(
                ClienteApi.unico(nombre), contexto.id("categoria:" + categoriaAlias)));
        contexto.id("template:" + nombre, respuesta.path("id"));
    }

    @Cuando("registro un template llamado {string} con una categoría inexistente")
    public void registrarTemplateConCategoriaInexistente(String nombre) {
        api.post("/templates", CatalogoDatos.templateConCategoriaInexistente(
                ClienteApi.unico(nombre), UUID.randomUUID().toString()));
    }

    @Cuando("registro la categoría {string}")
    public void registrarCategoria(String nombre) {
        api.post("/categorias", CatalogoDatos.categoria(nombre));
    }

    @Cuando("renombro la categoría {string} a {string}")
    public void renombrarCategoria(String categoriaAlias, String nuevoNombre) {
        api.put("/categorias/" + contexto.id("categoria:" + categoriaAlias),
                CatalogoDatos.renombrarCategoria(nuevoNombre));
    }

    @Cuando("registro una variante con SKU {string} para el template {string}")
    public void registrarVariante(String sku, String templateAlias) {
        Response respuesta = api.post("/variantes", CatalogoDatos.variante(
                sku, "M", "Negro", contexto.id("template:" + templateAlias)));
        contexto.id("variante:" + sku, respuesta.path("id"));
    }

    @Entonces("el campo {string} es el identificador de la categoría {string}")
    public void elCampoEsElIdentificadorDeLaCategoria(String ruta, String categoriaAlias) {
        contexto.getRespuesta().then().body(ruta, equalTo(contexto.id("categoria:" + categoriaAlias)));
    }
}
