package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-11 — Registrar Centro de Distribución. Casos CP-HU11-001 a CP-HU11-003.
 */
class Hu11RegistrarCentroDistribucionIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-HU11-001 — Registrar Centro de Distribución con la información requerida")
    void cpHu11001_registrarCentroDistribucionValido() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String nombre = unico("CD HU11-001");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Itagüí, Antioquia"}
                        """.formatted(nombre))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .body("nombre", equalTo(nombre))
                .body("ubicacion", equalTo("Itagüí, Antioquia"));
    }

    @Test
    @DisplayName("CP-HU11-002 — Rechazar registro de CD con campos obligatorios incompletos")
    void cpHu11002_rechazarCamposIncompletos() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "", "ubicacion": "Sin nombre"}
                        """)
                .when().post("/centros-distribucion")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("CP-HU11-003 — Rechazar registro de Centro de Distribución duplicado")
    void cpHu11003_rechazarCentroDistribucionDuplicado() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String nombre = unico("CD HU11-003");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Bogotá"}
                        """.formatted(nombre))
                .when().post("/centros-distribucion")
                .then().statusCode(201);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Otra ubicación"}
                        """.formatted(nombre))
                .when().post("/centros-distribucion")
                .then().statusCode(409);
    }
}
