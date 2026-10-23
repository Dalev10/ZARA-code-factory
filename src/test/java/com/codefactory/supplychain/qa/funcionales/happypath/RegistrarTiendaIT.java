package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Tag("funcional")
@Tag("happy-path")
class RegistrarTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — Registrar Tienda con información válida y ubicación")
    void cpHu03001_registrarTiendaValidaConUbicacion() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String nombre = unico("Tienda HU03-001");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Medellín, Antioquia"}
                        """.formatted(nombre))
                .when().post("/tiendas")
                .then().statusCode(201)
                .body("nombre", equalTo(nombre))
                .body("ubicacion", equalTo("Medellín, Antioquia"))
                .body("estado", equalTo("ACTIVA"));
    }
}
