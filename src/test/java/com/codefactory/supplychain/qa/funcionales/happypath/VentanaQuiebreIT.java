package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU-45 y HU-46 (Sprint 2). Una ventana se abre cuando el Almacén cruza de > 0 a ≤ 0 y se cierra
 * con REPOSICION al volver a > 0. Una venta forzada la cierra con VENTA_FORZADA y nunca la abre.
 * Desactivar la tienda la cierra con TIENDA_DESACTIVADA. Una carga en 0 no abre ventana. La
 * reconciliación ejecutada dos veces no cambia nada la segunda vez.
 */
@Tag("funcional")
@Tag("happy-path")
class VentanaQuiebreIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Una venta que deja el Almacén en 0 abre una ventana de quiebre en la misma respuesta")
    void cpHu45001_ventaAbreVentana() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU45-001");
        Variante variante = crearVariante(tokenAdmin, "HU45-001");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 2);

        vender(tokenAdmin, tienda.id(), variante.sku(), 2);

        List<Map<String, Object>> v = ventanas(tokenAdmin, tienda.id(), variante.id());
        assertEquals(1, v.size(), "debe existir una ventana");
        assertEquals("ABIERTA", v.get(0).get("estado"));
        assertTrue(((String) v.get(0).get("fechaInicio")).endsWith("Z"));
    }

    @Test
    @DisplayName("CP-002 — Reponer el Almacén por encima de 0 cierra la ventana con REPOSICION")
    void cpHu45002_reposicionCierraVentana() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU45-002");
        Variante variante = crearVariante(tokenAdmin, "HU45-002");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        vender(tokenAdmin, tienda.id(), variante.sku(), 1);

        ajustar(tokenAdmin, tienda.almacenId(), variante.sku(), 5, "INGRESO_MANUAL", null).then().statusCode(201);

        List<Map<String, Object>> v = ventanas(tokenAdmin, tienda.id(), variante.id());
        assertEquals(1, v.size());
        assertEquals("CERRADA", v.get(0).get("estado"));
        assertEquals("REPOSICION", v.get(0).get("motivoCierre"));
    }

    @Test
    @DisplayName("CP-008 — Consultar una ventana por id y filtrar por abiertas")
    void cpHu46001_consultarVentanaPorIdYFiltro() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU46-001");
        Variante variante = crearVariante(tokenAdmin, "HU46-001");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        vender(tokenAdmin, tienda.id(), variante.sku(), 1);
        String ventanaId = (String) ventanas(tokenAdmin, tienda.id(), variante.id()).get(0).get("id");

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/ventanas-quiebre/" + ventanaId)
                .then().statusCode(200)
                .body("id", equalTo(ventanaId))
                .body("tiendaId", equalTo(tienda.id()))
                .body("varianteId", equalTo(variante.id()));

        given().header("Authorization", bearer(tokenAdmin))
                .queryParam("tiendaId", tienda.id())
                .queryParam("abierta", true)
                .when().get("/ventanas-quiebre")
                .then().statusCode(200)
                .body("content.size()", equalTo(1));
    }
}
