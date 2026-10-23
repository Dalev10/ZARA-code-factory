package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * HU-34 (Sprint 2, D-55). La tienda nace con su Almacén y su Bodega_Tienda; sus ids se obtienen
 * de POST /tiendas o GET /tiendas/{id}. El listado no trae los nodos.
 */
@Tag("funcional")
@Tag("happy-path")
class NodosDeTiendaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Crear una tienda devuelve los ids de su Almacén y su Bodega")
    void cpHu34001_crearTiendaExponeAlmacenYBodega() {
        String tokenAdmin = iniciarSesionComoAdmin();

        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU34-001");

        org.junit.jupiter.api.Assertions.assertNotNull(tienda.almacenId(), "almacenId");
        org.junit.jupiter.api.Assertions.assertNotNull(tienda.bodegaId(), "bodegaId");
        org.junit.jupiter.api.Assertions.assertNotEquals(tienda.almacenId(), tienda.bodegaId());
    }

    @Test
    @DisplayName("CP-002 — GET /tiendas/{id} devuelve los mismos ids de nodo que la creación")
    void cpHu34002_consultarTiendaExponeLosMismosNodos() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU34-002");

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas/" + tienda.id())
                .then().statusCode(200)
                .body("almacenId", equalTo(tienda.almacenId()))
                .body("bodegaId", equalTo(tienda.bodegaId()));
    }

    @Test
    @DisplayName("CP-003 — La Bodega consultada por tienda coincide con el bodegaId de la tienda")
    void cpHu34003_bodegaPorTiendaCoincideConBodegaId() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU34-003");

        org.junit.jupiter.api.Assertions.assertEquals(tienda.bodegaId(), bodegaDeTienda(tokenAdmin, tienda.id()));
    }

    @Test
    @DisplayName("CP-004 — El listado de tiendas no trae los ids de nodo")
    void cpHu34004_listadoDeTiendasSinNodos() {
        String tokenAdmin = iniciarSesionComoAdmin();
        crearTienda(tokenAdmin, "Tienda HU34-004");

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas")
                .then().statusCode(200)
                .body("content[0].id", notNullValue())
                .body("content[0].almacenId", nullValue());
    }

    @Test
    @DisplayName("CP-005 — El Centro de Distribución expone el id de su nodo en nodo.id")
    void cpHu34005_cdExponeIdDeNodo() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String cdId = given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Girardota, Antioquia"}
                        """.formatted(unico("CD HU34-005")))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/centros-distribucion/" + cdId)
                .then().statusCode(200)
                .body("nodo.id", notNullValue())
                .body("nodo.tipo", equalTo("CD"))
                .body("nodo.cdId", equalTo(cdId));
    }
}
