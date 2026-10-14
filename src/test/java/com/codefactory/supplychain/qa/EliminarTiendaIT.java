package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class EliminarTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — Eliminar (desactivar) Tienda sin dependencias")
    void cpHu06001_eliminarTiendaSinDependencias() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Envigado, Antioquia"}
                        """.formatted(unico("Tienda HU06-001")))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/tiendas/" + tiendaId)
                .then().statusCode(204);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/tiendas/" + tiendaId)
                .then().statusCode(200)
                .body("estado", equalTo("INACTIVA"));

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/bodegas-tienda")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("CP-002 — Bloquear eliminación de Tienda con Bodega_Tienda asociada [defecto D-01]")
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
        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/bodegas-tienda")
                .then().statusCode(201);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/tiendas/" + tiendaId);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/tiendas/" + tiendaId)
                .then().statusCode(200)
                .body("estado", equalTo("ACTIVA"));
    }

    @Test
    @DisplayName("CP-003 — Validar que existen operaciones/información asociada antes de eliminar [defecto D-01]")
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
        String bodegaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/bodegas-tienda")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/tiendas/" + tiendaId);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/" + bodegaId)
                .then().statusCode(200)
                .body("tiendaId", equalTo(tiendaId));
        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/tiendas/" + tiendaId)
                .then().statusCode(200)
                .body("estado", equalTo("ACTIVA"));
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
