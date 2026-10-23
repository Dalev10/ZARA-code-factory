package com.codefactory.supplychain.qa.nofuncionales.fiabilidad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * HU-29 (Sprint 2). Contrato transversal de errores de la guía: todo error trae
 * {"mensaje": "..."} en español neutro y llega con su código real (un 404 o 405 con usuario
 * autenticado ya no se convierte en 401). Las fechas se responden en UTC con Z.
 */
@Tag("no-funcional")
@Tag("fiabilidad")
class RespuestasDeErrorIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Un JSON mal formado responde 400 con mensaje, no con cuerpo vacío")
    void cpHu29001_jsonMalFormadoResponde400ConMensaje() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("{\"nombre\": \"Tienda rota\", ")
                .when().post("/tiendas")
                .then().statusCode(400)
                .body("mensaje", not(emptyOrNullString()));
    }

    @Test
    @DisplayName("CP-003 — Un recurso inexistente con usuario autenticado responde 404, no 401")
    void cpHu29003_recursoInexistenteResponde404() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas/" + UUID.randomUUID())
                .then().statusCode(404)
                .body("mensaje", notNullValue());
    }

    @Test
    @DisplayName("CP-004 — Un método no permitido con usuario autenticado responde 405, no 401")
    void cpHu29004_metodoNoPermitidoResponde405() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("{}")
                .when().post("/bodegas-tienda")
                .then().statusCode(405)
                .body("mensaje", notNullValue());
    }

    @Test
    @DisplayName("CP-007 — Un id con formato inválido en la ruta responde 400 con mensaje")
    void cpHu29007_idConFormatoInvalidoResponde400() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/tiendas/no-es-un-uuid")
                .then().statusCode(400)
                .body("mensaje", notNullValue());
    }
}
