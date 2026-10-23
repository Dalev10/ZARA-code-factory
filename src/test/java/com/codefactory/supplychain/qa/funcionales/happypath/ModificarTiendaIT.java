package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Tag("funcional")
@Tag("happy-path")
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
    @DisplayName("CP-001 — Modificar Tienda con datos válidos")
    void cpHu05001_modificarTiendaConDatosValidos() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);
        String nuevoNombre = unico("Tienda HU05 Actualizada");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Sabaneta, Antioquia"}
                        """.formatted(nuevoNombre))
                .when().put("/tiendas/" + tiendaId)
                .then().statusCode(200)
                .body("nombre", equalTo(nuevoNombre))
                .body("ubicacion", equalTo("Sabaneta, Antioquia"));
    }
}
