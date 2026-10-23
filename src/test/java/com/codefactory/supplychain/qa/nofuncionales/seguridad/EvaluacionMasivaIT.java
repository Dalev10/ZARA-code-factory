package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * HU-50 (Sprint 2). La evaluación masiva revisa todas las tiendas activas, o una, y ejecuta los
 * traspasos que correspondan. Sirve para reponer después de subir un mínimo.
 */
@Tag("no-funcional")
@Tag("seguridad")
class EvaluacionMasivaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-006 — La evaluación masiva exige reposicion:administrar (403 para un supervisor)")
    void cpHu50006_exigeScope() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Usuario supervisor = usuarioConRol(tokenAdmin, "hu50-006", "SUPERVISOR_TIENDA");

        given().header("Authorization", bearer(supervisor.token()))
                .when().post("/reposicion-interna/evaluar")
                .then().statusCode(403);
    }
}
