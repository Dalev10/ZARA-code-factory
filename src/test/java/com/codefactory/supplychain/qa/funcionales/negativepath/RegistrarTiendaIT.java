package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@Tag("funcional")
@Tag("negative-path")
class RegistrarTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-002 — Rechazar registro de Tienda con información obligatoria incompleta")
    void cpHu03002_rechazarInformacionIncompleta() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "", "ubicacion": "Sin nombre"}
                        """)
                .when().post("/tiendas")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("CP-003 — Rechazar registro de Tienda duplicada")
    void cpHu03003_rechazarTiendaDuplicada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String nombre = unico("Tienda HU03-003");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Bogotá"}
                        """.formatted(nombre))
                .when().post("/tiendas")
                .then().statusCode(201);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Otra ubicación"}
                        """.formatted(nombre))
                .when().post("/tiendas")
                .then().statusCode(409);
    }
}
