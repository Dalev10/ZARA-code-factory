package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-09 — Modificar Bodega_Tienda. Casos CP-HU09-001 a CP-HU09-003.
 */
class Hu09ModificarBodegaTiendaIT extends AbstractCasoPruebaIT {

    private static String bodegaId;
    private static String tiendaDestinoId;

    private void asegurarFixture(String tokenAdmin) {
        if (bodegaId == null) {
            String tiendaOrigenId = given().header("Authorization", "Bearer " + tokenAdmin)
                    .contentType("application/json")
                    .body("""
                            {"nombre": "%s", "ubicacion": "Girardota, Antioquia"}
                            """.formatted(unico("Tienda HU09 Origen")))
                    .when().post("/tiendas")
                    .then().statusCode(201)
                    .extract().path("id");
            bodegaId = given().header("Authorization", "Bearer " + tokenAdmin)
                    .contentType("application/json")
                    .body("""
                            {"tiendaId": "%s"}
                            """.formatted(tiendaOrigenId))
                    .when().post("/bodegas-tienda")
                    .then().statusCode(201)
                    .extract().path("id");
            tiendaDestinoId = given().header("Authorization", "Bearer " + tokenAdmin)
                    .contentType("application/json")
                    .body("""
                            {"nombre": "%s", "ubicacion": "Barbosa, Antioquia"}
                            """.formatted(unico("Tienda HU09 Destino")))
                    .when().post("/tiendas")
                    .then().statusCode(201)
                    .extract().path("id");
        }
    }

    @Test
    @DisplayName("CP-HU09-001 — Modificar Bodega_Tienda reasignándola a otra Tienda existente")
    void cpHu09001_modificarBodegaReasignandoATiendaExistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaDestinoId))
                .when().put("/bodegas-tienda/" + bodegaId)
                .then().statusCode(200)
                .body("tiendaId", equalTo(tiendaDestinoId));
    }

    @Test
    @DisplayName("CP-HU09-002 — Rechazar modificación de Bodega_Tienda a una Tienda inexistente")
    void cpHu09002_rechazarModificacionATiendaInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(UUID.randomUUID()))
                .when().put("/bodegas-tienda/" + bodegaId)
                .then().statusCode(404);
    }

    @Test
    @DisplayName("CP-HU09-003 — Rechazar modificación de Bodega_Tienda con información inválida")
    void cpHu09003_rechazarModificacionInvalida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": null}
                        """)
                .when().put("/bodegas-tienda/" + bodegaId)
                .then().statusCode(400);
    }
}
