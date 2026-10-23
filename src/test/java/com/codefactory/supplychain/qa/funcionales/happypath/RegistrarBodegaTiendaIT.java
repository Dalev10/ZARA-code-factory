package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-07 (Sprint 1) actualizada por D-55 (HU-34, Sprint 2): la Bodega_Tienda ya no se
 * registra por API, se crea junto con su tienda.
 */
@Tag("funcional")
@Tag("happy-path")
class RegistrarBodegaTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — La Bodega_Tienda se crea automáticamente al registrar la Tienda")
    void cpHu07001_registrarBodegaTiendaValida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Rionegro, Antioquia"}
                        """.formatted(unico("Tienda HU07-001")))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/tienda/" + tiendaId)
                .then().statusCode(200)
                .body("tiendaId", equalTo(tiendaId));
    }
}
