package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-13 — Modificar Centro de Distribución. Casos CP-HU13-001 y CP-HU13-002.
 */
class Hu13ModificarCentroDistribucionIT extends AbstractCasoPruebaIT {

    private static String cdId;

    private void asegurarFixture(String tokenAdmin) {
        if (cdId == null) {
            cdId = given().header("Authorization", "Bearer " + tokenAdmin)
                    .contentType("application/json")
                    .body("""
                            {"nombre": "%s", "ubicacion": "Caldas, Antioquia"}
                            """.formatted(unico("CD HU13")))
                    .when().post("/centros-distribucion")
                    .then().statusCode(201)
                    .extract().path("id");
        }
    }

    @Test
    @DisplayName("CP-HU13-001 — Modificar Centro de Distribución con datos válidos")
    void cpHu13001_modificarCentroDistribucionConDatosValidos() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);
        String nuevoNombre = unico("CD HU13 Actualizado");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Envigado, Antioquia"}
                        """.formatted(nuevoNombre))
                .when().put("/centros-distribucion/" + cdId)
                .then().statusCode(200)
                .body("nombre", equalTo(nuevoNombre))
                .body("ubicacion", equalTo("Envigado, Antioquia"));
    }

    @Test
    @DisplayName("CP-HU13-002 — Rechazar modificación de CD con información inválida")
    void cpHu13002_rechazarModificacionInvalida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "", "ubicacion": "No debería guardarse"}
                        """)
                .when().put("/centros-distribucion/" + cdId)
                .then().statusCode(400);
    }
}
