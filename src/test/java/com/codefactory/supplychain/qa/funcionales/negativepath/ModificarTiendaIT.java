package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@Tag("funcional")
@Tag("negative-path")
class ModificarTiendaIT extends AbstractCasoPruebaIT {

    private static String tiendaId;
    private static String bodegaId;

    private void asegurarFixture(String tokenAdmin) {
        if (tiendaId == null) {
            tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                    .contentType("application/json")
                    .body("""
                            {"nombre": "%s", "ubicacion": "Itagüí, Antioquia"}
                            """.formatted(unico("Tienda HU05")))
                    .when().post("/tiendas")
                    .then().statusCode(201)
                    .extract().path("id");

            bodegaId = bodegaDeTienda(tokenAdmin, tiendaId);
        }
    }

    @Test
    @DisplayName("CP-002 — Rechazar modificación de Tienda con información inválida")
    void cpHu05002_rechazarModificacionInvalida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "", "ubicacion": "No debería guardarse"}
                        """)
                .when().put("/tiendas/" + tiendaId)
                .then().statusCode(400);
    }
}
