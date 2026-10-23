package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HU-50 (Sprint 2). La evaluación masiva revisa todas las tiendas activas, o una, y ejecuta los
 * traspasos que correspondan. Sirve para reponer después de subir un mínimo.
 */
@Tag("funcional")
@Tag("exceptional-path")
class EvaluacionMasivaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-002 — Evaluar dos veces seguidas no ejecuta traspasos la segunda vez")
    void cpHu50002_segundaEvaluacionSinCambios() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU50-002");
        Variante variante = crearVariante(tokenAdmin, "HU50-002");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 20);
        crearUmbralVariante(tokenAdmin, variante.id(), 3, 6);

        given().header("Authorization", bearer(tokenAdmin))
                .when().post("/reposicion-interna/tiendas/" + tienda.id() + "/evaluar")
                .then().statusCode(200)
                .body("traspasoEjecutados", equalTo(1));
        given().header("Authorization", bearer(tokenAdmin))
                .when().post("/reposicion-interna/tiendas/" + tienda.id() + "/evaluar")
                .then().statusCode(200)
                .body("traspasoEjecutados", equalTo(0));

        assertEquals(6, stock(tokenAdmin, tienda.almacenId(), variante.id()));
    }

    @Test
    @DisplayName("CP-004 — La evaluación masiva omite las tiendas inactivas")
    void cpHu50004_omiteTiendasInactivas() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Tienda tienda = crearTienda(tokenAdmin, "Tienda HU50-004");
        Variante variante = crearVariante(tokenAdmin, "HU50-004");
        cargar(tokenAdmin, tienda.almacenId(), variante.sku(), 1);
        cargar(tokenAdmin, tienda.bodegaId(), variante.sku(), 10);
        crearUmbralVariante(tokenAdmin, variante.id(), 3, 5);
        given().header("Authorization", bearer(tokenAdmin))
                .when().delete("/tiendas/" + tienda.id())
                .then().statusCode(204);

        given().header("Authorization", bearer(tokenAdmin))
                .when().post("/reposicion-interna/evaluar")
                .then().statusCode(200);

        assertEquals(1, stock(tokenAdmin, tienda.almacenId(), variante.id()));
        assertEquals(10, stock(tokenAdmin, tienda.bodegaId(), variante.id()));
    }
}
