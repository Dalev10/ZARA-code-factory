package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-41 (Sprint 2). Anular exige {"motivo"}; el stock vuelve al Almacén. Anular de nuevo responde
 * 200 con la anulación original y no devuelve stock otra vez. Por transacción es todo o nada.
 */
@Tag("funcional")
@Tag("happy-path")
class AnularVentaIT extends AbstractCasoPruebaSprint2IT {

    private Response anular(String token, String ruta, String motivoJson) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body(motivoJson)
                .when().post(ruta);
    }

    @Test
    @DisplayName("CP-001 — Anular una línea de venta devuelve el stock al Almacén y registra el motivo")
    void cpHu41001_anularLineaDevuelveStock() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU41-001");
        Variante variante = crearVariante(tokenAdmin, "HU41-001");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 10);
        Usuario supervisor = usuarioConRol(tokenAdmin, "hu41-001", "SUPERVISOR_TIENDA");
        asignarTienda(tokenAdmin, supervisor.id(), tienda.id());
        String lineaId = vender(supervisor.token(), tienda.id(), variante.sku(), 4).path("lineas[0].id");

        anular(supervisor.token(), "/ventas/" + lineaId + "/anular", "{\"motivo\": \"Cliente desistió\"}")
                .then().statusCode(200)
                .body("lineas[0].estado", equalTo("ANULADA"))
                .body("lineas[0].motivoAnulacion", equalTo("Cliente desistió"))
                .body("lineas[0].anuladaPor", equalTo(supervisor.id()))
                .body("lineas[0].anuladaEn", notNullValue());

        assertEquals(10, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-004 — Anular por transacción anula todas sus líneas y devuelve todo el stock")
    void cpHu41004_anularTransaccionCompleta() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU41-004");
        Variante a = crearVariante(tokenAdmin, "HU41-004A");
        Variante b = crearVariante(tokenAdmin, "HU41-004B");
        cargar(tokenAdmin, tienda.almacenId(), a.sku(), 10);
        cargar(tokenAdmin, tienda.almacenId(), b.sku(), 10);
        String transaccionId = UUID.randomUUID().toString();
        registrarVenta(tokenAdmin, cuerpoVenta(transaccionId, tienda.id(), false,
                linea(a.sku(), 2), linea(b.sku(), 5)))
                .then().statusCode(201);

        anular(tokenAdmin, "/ventas/transacciones/" + transaccionId + "/anular", "{\"motivo\": \"Devolución total\"}")
                .then().statusCode(200)
                .body("lineas.estado", everyItem(equalTo("ANULADA")));

        assertEquals(10, stock(tokenAdmin, tienda.almacenId(), a.id()));
        assertEquals(10, stock(tokenAdmin, tienda.almacenId(), b.id()));
    }
}
