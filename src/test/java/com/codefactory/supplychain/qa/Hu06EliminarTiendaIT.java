package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-06 — Eliminar Tienda/Almacén. Casos CP-HU06-001 a CP-HU06-004.
 *
 * <p>CP-HU06-002 y CP-HU06-003 documentan el defecto D-01 (severidad Alta):
 * {@code TiendaService.desactivar()} no valida si la Tienda tiene una
 * Bodega_Tienda asociada antes de desactivarla. Estas dos pruebas afirman
 * el criterio de aceptación DOCUMENTADO (AC-2/AC-3/AC-4: la eliminación no
 * debe ejecutarse), así que quedarán en rojo hasta que ese defecto se
 * corrija — es la señal correcta y esperada de un defecto real, no un
 * error de la prueba.
 */
class Hu06EliminarTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-HU06-001 — Eliminar (desactivar) Tienda sin dependencias")
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

        // AC-5: una Tienda eliminada no debe quedar disponible para nuevas operaciones.
        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/bodegas-tienda")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("CP-HU06-002 — Bloquear eliminación de Tienda con Bodega_Tienda asociada [defecto D-01]")
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

        // AC-2 / AC-4: al tener una Bodega_Tienda asociada, la eliminación NO debía
        // ejecutarse y la Tienda debía seguir ACTIVA. Hoy el sistema la desactiva
        // igual (204) — este assert documenta el defecto D-01.
        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/tiendas/" + tiendaId)
                .then().statusCode(200)
                .body("estado", equalTo("ACTIVA"));
    }

    @Test
    @DisplayName("CP-HU06-003 — Validar que existen operaciones/información asociada antes de eliminar [defecto D-01]")
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

        // AC-3: la relación válida con su Bodega_Tienda debe permanecer intacta.
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
    @DisplayName("CP-HU06-004 — Conservar información histórica tras eliminar una Tienda")
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

        // Al ser una baja lógica (desactivación), el registro sigue disponible.
        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/tiendas/" + tiendaId)
                .then().statusCode(200)
                .body("nombre", equalTo(nombre))
                .body("estado", equalTo("INACTIVA"));
    }
}
