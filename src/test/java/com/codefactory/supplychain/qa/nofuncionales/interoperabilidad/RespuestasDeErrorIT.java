package com.codefactory.supplychain.qa.nofuncionales.interoperabilidad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;

/**
 * HU-29 (Sprint 2). Contrato transversal de errores de la guía: todo error trae
 * {"mensaje": "..."} en español neutro y llega con su código real (un 404 o 405 con usuario
 * autenticado ya no se convierte en 401). Las fechas se responden en UTC con Z.
 */
@Tag("no-funcional")
@Tag("interoperabilidad")
class RespuestasDeErrorIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-006 — Las fechas se responden en UTC (ISO-8601 con Z)")
    void cpHu29006_fechasEnUtc() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU29-006");

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas/" + tienda.id())
                .then().statusCode(200)
                .body("creadoEn", endsWith("Z"));
    }
}
