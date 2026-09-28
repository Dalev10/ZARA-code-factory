package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-03 — Registrar Tienda/Almacén. Casos CP-HU03-001 a CP-HU03-003.
 */
class Hu03RegistrarTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-HU03-001 — Registrar Tienda con información válida y ubicación")
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

    @Test
    @DisplayName("CP-HU03-002 — Rechazar registro de Tienda con información obligatoria incompleta")
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
    @DisplayName("CP-HU03-003 — Rechazar registro de Tienda duplicada")
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
