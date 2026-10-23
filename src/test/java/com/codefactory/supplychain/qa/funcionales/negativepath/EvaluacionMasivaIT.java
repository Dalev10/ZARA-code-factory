package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * HU-50 (Sprint 2). La evaluación masiva revisa todas las tiendas activas, o una, y ejecuta los
 * traspasos que correspondan. Sirve para reponer después de subir un mínimo.
 */
@Tag("funcional")
@Tag("negative-path")
class EvaluacionMasivaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-005 — Evaluar una tienda inexistente responde 404")
    void cpHu50005_tiendaInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .when().post("/reposicion-interna/tiendas/" + java.util.UUID.randomUUID() + "/evaluar")
                .then().statusCode(404);
    }
}
