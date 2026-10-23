package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-06 (Sprint 1). El defecto D-01 (bloquear la desactivación de una Tienda con Bodega) quedó
 * superado por el Sprint 2: toda Tienda tiene Bodega (D-55) y desactivarla es una baja lógica
 * que conserva su información y cierra sus ventanas de quiebre (D-50).
 */
@Tag("funcional")
@Tag("exceptional-path")
class EliminarTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-002 — Desactivar una Tienda con Bodega_Tienda la deja INACTIVA (D-55, D-50)")
    void cpHu06002_bloquearEliminacionConBodegaAsociada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Bello, Antioquia"}
                        """.formatted(unico("Tienda HU06-002")))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");
        bodegaDeTienda(tokenAdmin, tiendaId);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/tiendas/" + tiendaId)
                .then().statusCode(204);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/tiendas/" + tiendaId)
                .then().statusCode(200)
                .body("estado", equalTo("INACTIVA"));
    }

    @Test
    @DisplayName("CP-003 — Desactivar una Tienda conserva su Bodega_Tienda y su información (baja lógica)")
    void cpHu06003_validarInformacionAsociadaAntesDeEliminar() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "La Estrella, Antioquia"}
                        """.formatted(unico("Tienda HU06-003")))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");
        String bodegaId = bodegaDeTienda(tokenAdmin, tiendaId);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/tiendas/" + tiendaId)
                .then().statusCode(204);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/" + bodegaId)
                .then().statusCode(200)
                .body("tiendaId", equalTo(tiendaId));
        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/tiendas/" + tiendaId)
                .then().statusCode(200)
                .body("estado", equalTo("INACTIVA"));
    }

    @Test
    @DisplayName("CP-004 — Conservar información histórica tras eliminar una Tienda")
    void cpHu06004_conservarInformacionHistorica() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String nombre = unico("Tienda HU06-004");
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Caldas, Antioquia"}
                        """.formatted(nombre))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/tiendas/" + tiendaId)
                .then().statusCode(204);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/tiendas/" + tiendaId)
                .then().statusCode(200)
                .body("nombre", equalTo(nombre))
                .body("estado", equalTo("INACTIVA"));
    }
}
