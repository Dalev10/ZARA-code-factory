package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * HU-45 y HU-46 (Sprint 2). Una ventana se abre cuando el Almacén cruza de > 0 a ≤ 0 y se cierra
 * con REPOSICION al volver a > 0. Una venta forzada la cierra con VENTA_FORZADA y nunca la abre.
 * Desactivar la tienda la cierra con TIENDA_DESACTIVADA. Una carga en 0 no abre ventana. La
 * reconciliación ejecutada dos veces no cambia nada la segunda vez.
 */
@Tag("funcional")
@Tag("negative-path")
class VentanaQuiebreIT extends AbstractCasoPruebaSprint2IT {

    private Response reconciliar(String token, String tiendaId) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/ventanas-quiebre/reconciliar");
    }

    @Test
    @DisplayName("CP-012 — Reconciliar sin tiendaId se rechaza (400)")
    void cpHu46005_reconciliarSinTienda() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json").body("{}")
                .when().post("/ventanas-quiebre/reconciliar")
                .then().statusCode(400)
                .body("mensaje", notNullValue());
    }
}
