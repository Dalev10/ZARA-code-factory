package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-41 (Sprint 2). Anular exige {"motivo"}; el stock vuelve al Almacén. Anular de nuevo responde
 * 200 con la anulación original y no devuelve stock otra vez. Por transacción es todo o nada.
 */
@Tag("funcional")
@Tag("exceptional-path")
class AnularVentaIT extends AbstractCasoPruebaSprint2IT {

    private Response anular(String token, String ruta, String motivoJson) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body(motivoJson)
                .when().post(ruta);
    }

    @Test
    @DisplayName("CP-003 — Anular dos veces responde 200 con la anulación original y no devuelve stock doble")
    void cpHu41003_anulacionRepetidaIdempotente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU41-003");
        Variante variante = crearVariante(tokenAdmin, "HU41-003");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);
        String lineaId = vender(tokenAdmin, tienda.id(), variante.sku(), 3).path("lineas[0].id");

        anular(tokenAdmin, "/ventas/" + lineaId + "/anular", "{\"motivo\": \"Primera\"}")
                .then().statusCode(200);
        anular(tokenAdmin, "/ventas/" + lineaId + "/anular", "{\"motivo\": \"Segunda\"}")
                .then().statusCode(200)
                .body("lineas[0].motivoAnulacion", equalTo("Primera"));

        assertEquals(10, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-005 — Anular por transacción dos veces no devuelve stock otra vez")
    void cpHu41005_anularTransaccionRepetida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU41-005");
        Variante variante = crearVariante(tokenAdmin, "HU41-005");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 6);
        String transaccionId = UUID.randomUUID().toString();
        registrarVenta(tokenAdmin, cuerpoVenta(transaccionId, tienda.id(), false, linea(variante.sku(), 2)))
                .then().statusCode(201);
        String ruta = "/ventas/transacciones/" + transaccionId + "/anular";

        anular(tokenAdmin, ruta, "{\"motivo\": \"Error de caja\"}").then().statusCode(200);
        anular(tokenAdmin, ruta, "{\"motivo\": \"Error de caja\"}").then().statusCode(200);

        assertEquals(6, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }
}
