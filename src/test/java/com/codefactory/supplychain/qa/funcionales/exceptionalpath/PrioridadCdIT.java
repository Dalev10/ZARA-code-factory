package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

/**
 * HU-51 (Sprint 2). PUT /tiendas/{tiendaId}/prioridad-cd {"cdIds":[...]} reemplaza la lista
 * completa; el orden de la lista es la prioridad. Un CD con prioridades no se elimina (409 con
 * mensaje).
 */
@Tag("funcional")
@Tag("exceptional-path")
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
    @DisplayName("CP-003 — Una lista vacía deja la tienda sin prioridades y libera el CD")
    void cpHu51003_listaVaciaLiberaCd() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU51-003");
        String cd = crearCd(tokenAdmin, "CD HU51-003");
        reemplazar(tokenAdmin, tienda.id(), cd).then().statusCode(200);

        reemplazar(tokenAdmin, tienda.id()).then().statusCode(200);

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas/" + tienda.id() + "/prioridad-cd")
                .then().statusCode(200)
                .body("$", hasSize(0));
        given().header("Authorization", bearer(tokenAdmin))
                .when().delete("/centros-distribucion/" + cd)
                .then().statusCode(204);
    }
}
