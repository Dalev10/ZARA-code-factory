package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-37 (Sprint 2). Carga inicial por lote de 1 a 500 ítems, todo o nada. Cantidad ≥ 0 (un 0
 * cuenta como cargado). SKU inexistente o repetido rechaza el lote; una variante que ya tiene
 * fila responde 409.
 */
@Tag("funcional")
@Tag("happy-path")
class CargaInicialInventarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Cargar inventario inicial válido en el Almacén de una tienda")
    void cpHu37001_cargaInicialValida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU37-001");
        Variante a = crearVariante(tokenAdmin, "HU37-001A");
        Variante b = crearVariante(tokenAdmin, "HU37-001B");

        cargaInicial(tokenAdmin, tienda.almacenId(), item(a.sku(), 12) + "," + item(b.sku(), 8))
                .then().statusCode(201)
                .body("nodoId", equalTo(tienda.almacenId()))
                .body("filasCreadas", equalTo(2))
                .body("unidadesTotales", equalTo(20));

        assertEquals(12, stock(tokenAdmin, tienda.almacenId(), a.id()));
        assertEquals(8, stock(tokenAdmin, tienda.almacenId(), b.id()));
    }

    @Test
    @DisplayName("CP-008 — Cargar en la Bodega_Tienda y en un Centro de Distribución")
    void cpHu37008_cargarEnBodegaYCentroDistribucion() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU37-008");
        Variante variante = crearVariante(tokenAdmin, "HU37-008");
        String cdId = io.restassured.RestAssured.given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Copacabana, Antioquia"}
                        """.formatted(unico("CD HU37-008")))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");
        String nodoCd = io.restassured.RestAssured.given().header("Authorization", bearer(tokenAdmin))
                .when().get("/centros-distribucion/" + cdId)
                .then().statusCode(200)
                .extract().path("nodo.id");

        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 15);
        cargar(tokenAdmin, nodoCd, variante.sku(), 100);

        assertEquals(15, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
        assertEquals(100, stock(tokenAdmin, nodoCd, variante.id()));
    }
}
