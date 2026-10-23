package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@Tag("funcional")
@Tag("negative-path")
class ModificarCentroDistribucionIT extends AbstractCasoPruebaIT {

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
    @DisplayName("CP-002 — Rechazar modificación de CD con información inválida")
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
