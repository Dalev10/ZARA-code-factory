package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-10 (Sprint 1) actualizada por D-55 (HU-34, Sprint 2): sin Bodega_Tienda no hay
 * traspasos, así que no se elimina por API.
 */
@Tag("funcional")
@Tag("negative-path")
class EliminarBodegaTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — Eliminar una Bodega_Tienda por API ya no está permitido (405) y se conserva")
    void cpHu10001_eliminarBodegaSinInventarioAsociado() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "La Ceja, Antioquia"}
                        """.formatted(unico("Tienda HU10-001")))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");
        String bodegaId = bodegaDeTienda(tokenAdmin, tiendaId);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/bodegas-tienda/" + bodegaId)
                .then().statusCode(405);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/" + bodegaId)
                .then().statusCode(200)
                .body("id", equalTo(bodegaId));
    }
}
