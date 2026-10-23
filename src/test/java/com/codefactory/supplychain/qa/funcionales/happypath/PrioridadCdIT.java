package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

/**
 * HU-51 (Sprint 2). PUT /tiendas/{tiendaId}/prioridad-cd {"cdIds":[...]} reemplaza la lista
 * completa; el orden de la lista es la prioridad. Un CD con prioridades no se elimina (409 con
 * mensaje).
 */
@Tag("funcional")
@Tag("happy-path")
class PrioridadCdIT extends AbstractCasoPruebaSprint2IT {

    private String crearCd(String tokenAdmin, String prefijo) {
        return given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Envigado, Antioquia"}
                        """.formatted(unico(prefijo)))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");
    }

    private Response reemplazar(String token, String tiendaId, String... cdIds) {
        String lista = cdIds.length == 0 ? "" : "\"" + String.join("\", \"", cdIds) + "\"";
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body("""
                        {"cdIds": [%s]}
                        """.formatted(lista))
                .when().put("/tiendas/" + tiendaId + "/prioridad-cd");
    }

    @Test
    @DisplayName("CP-001 — Definir la prioridad de CDs respeta el orden de la lista")
    void cpHu51001_definirPrioridadEnOrden() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU51-001");
        String cd1 = crearCd(tokenAdmin, "CD HU51-001A");
        String cd2 = crearCd(tokenAdmin, "CD HU51-001B");
        String cd3 = crearCd(tokenAdmin, "CD HU51-001C");

        reemplazar(tokenAdmin, tienda.id(), cd2, cd3, cd1).then().statusCode(200);

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas/" + tienda.id() + "/prioridad-cd")
                .then().statusCode(200)
                .body("sort { it.ordenPrioridad }.cdId", contains(cd2, cd3, cd1));
    }

    @Test
    @DisplayName("CP-002 — Un PUT nuevo reemplaza la lista completa")
    void cpHu51002_reemplazarListaCompleta() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU51-002");
        String cd1 = crearCd(tokenAdmin, "CD HU51-002A");
        String cd2 = crearCd(tokenAdmin, "CD HU51-002B");
        reemplazar(tokenAdmin, tienda.id(), cd1, cd2).then().statusCode(200);

        reemplazar(tokenAdmin, tienda.id(), cd2).then().statusCode(200);

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas/" + tienda.id() + "/prioridad-cd")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("cdId", contains(cd2));
    }
}
