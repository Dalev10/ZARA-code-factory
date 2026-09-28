package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-05 — Modificar Tienda/Almacén. Casos CP-HU05-001 a CP-HU05-003.
 */
class Hu05ModificarTiendaIT extends AbstractCasoPruebaIT {

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

            bodegaId = given().header("Authorization", "Bearer " + tokenAdmin)
                    .contentType("application/json")
                    .body("""
                            {"tiendaId": "%s"}
                            """.formatted(tiendaId))
                    .when().post("/bodegas-tienda")
                    .then().statusCode(201)
                    .extract().path("id");
        }
    }

    @Test
    @DisplayName("CP-HU05-001 — Modificar Tienda con datos válidos")
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

    @Test
    @DisplayName("CP-HU05-002 — Rechazar modificación de Tienda con información inválida")
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

    @Test
    @DisplayName("CP-HU05-003 — La modificación de la Tienda no debe eliminar la información histórica relacionada")
    void cpHu05003_noEliminarInformacionHistoricaRelacionada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Envigado, Antioquia"}
                        """.formatted(unico("Tienda HU05 OtraVez")))
                .when().put("/tiendas/" + tiendaId)
                .then().statusCode(200);

        // La Bodega_Tienda asociada, creada antes de la modificación, debe seguir
        // consultable y seguir apuntando a la misma Tienda.
        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/" + bodegaId)
                .then().statusCode(200)
                .body("tiendaId", equalTo(tiendaId));
    }
}
