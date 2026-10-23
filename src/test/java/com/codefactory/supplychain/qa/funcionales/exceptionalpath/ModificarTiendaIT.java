package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Tag("funcional")
@Tag("exceptional-path")
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
    @DisplayName("CP-003 — La modificación de la Tienda no debe eliminar la información histórica relacionada")
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

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/" + bodegaId)
                .then().statusCode(200)
                .body("tiendaId", equalTo(tiendaId));
    }
}
