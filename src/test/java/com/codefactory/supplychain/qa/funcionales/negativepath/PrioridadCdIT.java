package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

/**
 * HU-51 (Sprint 2). PUT /tiendas/{tiendaId}/prioridad-cd {"cdIds":[...]} reemplaza la lista
 * completa; el orden de la lista es la prioridad. Un CD con prioridades no se elimina (409 con
 * mensaje).
 */
@Tag("funcional")
@Tag("negative-path")
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
    @DisplayName("CP-004 — Un CD con prioridades no se elimina (409 con mensaje)")
    void cpHu51004_cdConPrioridadNoSeElimina() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU51-004");
        String cd = crearCd(tokenAdmin, "CD HU51-004");
        reemplazar(tokenAdmin, tienda.id(), cd).then().statusCode(200);

        given().header("Authorization", bearer(tokenAdmin))
                .when().delete("/centros-distribucion/" + cd)
                .then().statusCode(409)
                .body("mensaje", containsString("prioridades"));
    }

    @Test
    @DisplayName("CP-005 — Rechazar un CD repetido o inexistente en la lista")
    void cpHu51005_rechazarCdRepetidoOInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU51-005");
        String cd = crearCd(tokenAdmin, "CD HU51-005");

        Response repetido = reemplazar(tokenAdmin, tienda.id(), cd, cd);
        org.junit.jupiter.api.Assertions.assertTrue(repetido.statusCode() == 400 || repetido.statusCode() == 409,
                "CD repetido: esperado 400 o 409, fue " + repetido.statusCode());
        reemplazar(tokenAdmin, tienda.id(), UUID.randomUUID().toString()).then().statusCode(404);

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas/" + tienda.id() + "/prioridad-cd")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("CP-006 — Rechazar el cuerpo sin cdIds (400) y una tienda inexistente (404)")
    void cpHu51006_validaciones() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU51-006");
        String cd = crearCd(tokenAdmin, "CD HU51-006");

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json").body("{}")
                .when().put("/tiendas/" + tienda.id() + "/prioridad-cd")
                .then().statusCode(400);
        reemplazar(tokenAdmin, UUID.randomUUID().toString(), cd).then().statusCode(404);
    }
}
