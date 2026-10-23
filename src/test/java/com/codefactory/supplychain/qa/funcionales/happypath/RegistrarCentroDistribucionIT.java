package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@Tag("funcional")
@Tag("happy-path")
class RegistrarCentroDistribucionIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — Registrar Centro de Distribución con la información requerida")
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
}
