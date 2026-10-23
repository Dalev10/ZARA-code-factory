package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-41 (Sprint 2). Anular exige {"motivo"}; el stock vuelve al Almacén. Anular de nuevo responde
 * 200 con la anulación original y no devuelve stock otra vez. Por transacción es todo o nada.
 */
@Tag("funcional")
@Tag("negative-path")
class AnularVentaIT extends AbstractCasoPruebaSprint2IT {

    private Response anular(String token, String ruta, String motivoJson) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body(motivoJson)
                .when().post(ruta);
    }

    @Test
    @DisplayName("CP-002 — Anular sin motivo se rechaza (400) y la venta sigue registrada")
    void cpHu41002_motivoObligatorio() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU41-002");
        Variante variante = crearVariante(tokenAdmin, "HU41-002");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);
        String lineaId = vender(tokenAdmin, tienda.id(), variante.sku(), 2).path("lineas[0].id");

        anular(tokenAdmin, "/ventas/" + lineaId + "/anular", "{}")
                .then().statusCode(400).body("mensaje", notNullValue());
        anular(tokenAdmin, "/ventas/" + lineaId + "/anular", "{\"motivo\": \"  \"}")
                .then().statusCode(400);

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/ventas/" + lineaId)
                .then().statusCode(200)
                .body("lineas[0].estado", equalTo("REGISTRADA"));
        assertEquals(8, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-006 — Anular una venta inexistente responde 404")
    void cpHu41006_anularVentaInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();

        anular(tokenAdmin, "/ventas/" + UUID.randomUUID() + "/anular", "{\"motivo\": \"No existe\"}")
                .then().statusCode(404);
        anular(tokenAdmin, "/ventas/transacciones/" + UUID.randomUUID() + "/anular", "{\"motivo\": \"No existe\"}")
                .then().statusCode(404);
    }
}
