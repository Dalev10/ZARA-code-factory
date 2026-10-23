package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-41 (Sprint 2). Anular exige {"motivo"}; el stock vuelve al Almacén. Anular de nuevo responde
 * 200 con la anulación original y no devuelve stock otra vez. Por transacción es todo o nada.
 */
@Tag("no-funcional")
@Tag("seguridad")
class AnularVentaIT extends AbstractCasoPruebaSprint2IT {

    private Response anular(String token, String ruta, String motivoJson) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body(motivoJson)
                .when().post(ruta);
    }

    @Test
    @DisplayName("CP-007 — Un supervisor no anula ventas de una tienda que no tiene asignada")
    void cpHu41007_supervisorEnTiendaAjena() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU41-007");
        Variante variante = crearVariante(tokenAdmin, "HU41-007");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 5);
        String lineaId = vender(tokenAdmin, tienda.id(), variante.sku(), 1).path("lineas[0].id");
        Usuario supervisor = usuarioConRol(tokenAdmin, "hu41-007", "SUPERVISOR_TIENDA");

        Response r = anular(supervisor.token(), "/ventas/" + lineaId + "/anular", "{\"motivo\": \"Ajena\"}");

        org.junit.jupiter.api.Assertions.assertTrue(r.statusCode() == 403 || r.statusCode() == 404,
                "esperado 403 o 404, fue " + r.statusCode());
        assertEquals(4, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }
}
