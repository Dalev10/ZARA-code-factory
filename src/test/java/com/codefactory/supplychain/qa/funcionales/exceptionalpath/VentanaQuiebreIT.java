package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-45 y HU-46 (Sprint 2). Una ventana se abre cuando el Almacén cruza de > 0 a ≤ 0 y se cierra
 * con REPOSICION al volver a > 0. Una venta forzada la cierra con VENTA_FORZADA y nunca la abre.
 * Desactivar la tienda la cierra con TIENDA_DESACTIVADA. Una carga en 0 no abre ventana. La
 * reconciliación ejecutada dos veces no cambia nada la segunda vez.
 */
@Tag("funcional")
@Tag("exceptional-path")
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
    @DisplayName("CP-003 — Una venta forzada cierra la ventana abierta con VENTA_FORZADA")
    void cpHu45003_ventaForzadaCierraVentana() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU45-003");
        Variante variante = crearVariante(tokenAdmin, "HU45-003");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        vender(tokenAdmin, tienda.id(), variante.sku(), 1);

        venderForzado(tokenAdmin, tienda.id(), variante.sku(), 1);

        List<Map<String, Object>> v = ventanas(tokenAdmin, tienda.id(), variante.id());
        assertEquals(1, v.size(), "la venta forzada no abre una ventana nueva");
        assertEquals("CERRADA", v.get(0).get("estado"));
        assertEquals("VENTA_FORZADA", v.get(0).get("motivoCierre"));
    }

    @Test
    @DisplayName("CP-004 — Una venta forzada que deja el Almacén en negativo no abre ventana")
    void cpHu45004_ventaForzadaNoAbreVentana() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU45-004");
        Variante variante = crearVariante(tokenAdmin, "HU45-004");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);

        venderForzado(tokenAdmin, tienda.id(), variante.sku(), 3);

        assertEquals(0, ventanas(tokenAdmin, tienda.id(), variante.id()).size());
    }

    @Test
    @DisplayName("CP-005 — Desactivar la tienda cierra sus ventanas con TIENDA_DESACTIVADA")
    void cpHu45005_desactivarTiendaCierraVentanas() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU45-005");
        Variante variante = crearVariante(tokenAdmin, "HU45-005");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        vender(tokenAdmin, tienda.id(), variante.sku(), 1);

        given().header("Authorization", bearer(tokenAdmin))
                .when().delete("/tiendas/" + tienda.id())
                .then().statusCode(204);

        List<Map<String, Object>> v = ventanas(tokenAdmin, tienda.id(), variante.id());
        assertEquals(1, v.size());
        assertEquals("CERRADA", v.get(0).get("estado"));
        assertEquals("TIENDA_DESACTIVADA", v.get(0).get("motivoCierre"));
    }

    @Test
    @DisplayName("CP-006 — Una carga inicial en 0 no abre ventana")
    void cpHu45006_cargaEnCeroNoAbreVentana() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU45-006");
        Variante variante = crearVariante(tokenAdmin, "HU45-006");

        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 0);

        assertEquals(0, ventanas(tokenAdmin, tienda.id(), variante.id()).size());
    }

    @Test
    @DisplayName("CP-007 — Anular la venta que abrió la ventana devuelve stock y la cierra con REPOSICION")
    void cpHu45007_anularVentaCierraVentana() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU45-007");
        Variante variante = crearVariante(tokenAdmin, "HU45-007");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        String lineaId = vender(tokenAdmin, tienda.id(), variante.sku(), 1).path("lineas[0].id");
        assertEquals("ABIERTA", ventanas(tokenAdmin, tienda.id(), variante.id()).get(0).get("estado"));

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json").body("{\"motivo\": \"Devolución\"}")
                .when().post("/ventas/" + lineaId + "/anular")
                .then().statusCode(200);

        List<Map<String, Object>> v = ventanas(tokenAdmin, tienda.id(), variante.id());
        assertEquals("CERRADA", v.get(0).get("estado"));
        assertEquals("REPOSICION", v.get(0).get("motivoCierre"));
    }

    @Test
    @DisplayName("CP-010 — Reconciliar dos veces: la segunda ejecución no cambia nada")
    void cpHu46003_reconciliacionIdempotente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU46-003");
        Variante variante = crearVariante(tokenAdmin, "HU46-003");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        vender(tokenAdmin, tienda.id(), variante.sku(), 1);
        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), 2, "INGRESO_MANUAL", null).then().statusCode(201);
        vender(tokenAdmin, tienda.id(), variante.sku(), 2);

        reconciliar(tokenAdmin, tienda.id()).then().statusCode(200).body("paresProcesados", notNullValue());
        List<Map<String, Object>> antes = ventanas(tokenAdmin, tienda.id(), variante.id());

        reconciliar(tokenAdmin, tienda.id()).then().statusCode(200)
                .body("creadas", equalTo(0))
                .body("cerradas", equalTo(0))
                .body("corregidas", equalTo(0));

        assertEquals(antes, ventanas(tokenAdmin, tienda.id(), variante.id()));
        assertEquals(2, antes.size(), "dos periodos de quiebre en el kardex");
    }
}
