package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-06 (Sprint 1). El defecto D-01 (bloquear la desactivación de una Tienda con Bodega) quedó
 * superado por el Sprint 2: toda Tienda tiene Bodega (D-55) y desactivarla es una baja lógica
 * que conserva su información y cierra sus ventanas de quiebre (D-50).
 */
@Tag("funcional")
@Tag("happy-path")
class EliminarTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — Eliminar (desactivar) Tienda sin dependencias")
    void cpHu06001_eliminarTiendaSinDependencias() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Envigado, Antioquia"}
                        """.formatted(unico("Tienda HU06-001")))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/tiendas/" + tiendaId)
                .then().statusCode(204);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/tiendas/" + tiendaId)
                .then().statusCode(200)
                .body("estado", equalTo("INACTIVA"));
    }
}
