package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * HU-37 (Sprint 2). Carga inicial por lote de 1 a 500 ítems, todo o nada. Cantidad ≥ 0 (un 0
 * cuenta como cargado). SKU inexistente o repetido rechaza el lote; una variante que ya tiene
 * fila responde 409.
 */
@Tag("no-funcional")
@Tag("seguridad")
class CargaInicialInventarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-009 — Un OPERADOR_CD no ve el nodo de una tienda (404) y un SUPERVISOR no carga en un CD (403)")
    void cpHu37009_scopePorTipoDeNodo() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU37-009");
        Variante variante = crearVariante(tokenAdmin, "HU37-009");
        String cdId = io.restassured.RestAssured.given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Barbosa, Antioquia"}
                        """.formatted(unico("CD HU37-009")))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");
        String nodoCd = io.restassured.RestAssured.given().header("Authorization", bearer(tokenAdmin))
                .when().get("/centros-distribucion/" + cdId)
                .then().statusCode(200)
                .extract().path("nodo.id");
        Usuario operadorCd = usuarioConRol(tokenAdmin, "hu37-009-op", "OPERADOR_CD");
        Usuario supervisor = usuarioConRol(tokenAdmin, "hu37-009-sup", "SUPERVISOR_TIENDA");
        asignarTienda(tokenAdmin, supervisor.id(), tienda.id());

        // Una tienda no asignada es un nodo ajeno: se oculta con 404 antes de revisar el scope.
        cargaInicial(operadorCd.token(), tienda.almacenId(), item(variante.sku(), 3)).then().statusCode(404);
        cargaInicial(supervisor.token(), nodoCd, item(variante.sku(), 3)).then().statusCode(403);

        cargaInicial(operadorCd.token(), nodoCd, item(variante.sku(), 3)).then().statusCode(201);
        cargaInicial(supervisor.token(), tienda.almacenId(), item(variante.sku(), 3)).then().statusCode(201);
    }

    @Test
    @DisplayName("CP-010 — Un SUPERVISOR no carga en el nodo de una tienda que no tiene asignada (404)")
    void cpHu37010_supervisorEnTiendaNoAsignada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU37-010");
        Variante variante = crearVariante(tokenAdmin, "HU37-010");
        Usuario supervisor = usuarioConRol(tokenAdmin, "hu37-010", "SUPERVISOR_TIENDA");

        cargaInicial(supervisor.token(), tienda.almacenId(), item(variante.sku(), 3)).then().statusCode(404);
        assertFalse(tieneFila(tokenAdmin, tienda.almacenId(), variante.id()));
    }
}
