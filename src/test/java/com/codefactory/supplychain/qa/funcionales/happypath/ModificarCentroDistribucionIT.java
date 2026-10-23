package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Tag("funcional")
@Tag("happy-path")
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
    @DisplayName("CP-001 — Modificar Centro de Distribución con datos válidos")
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
}
