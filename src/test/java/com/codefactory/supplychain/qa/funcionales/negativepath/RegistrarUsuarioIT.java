package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@Tag("funcional")
@Tag("negative-path")
class RegistrarUsuarioIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-002 — Rechazar registro de usuario con información obligatoria incompleta")
    void cpHu01002_rechazarRegistroInformacionIncompleta() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"email": "%s", "nombreCompleto": "", "password": "ClaveSegura2026!"}
                        """.formatted(unico("hu01-002") + "@supplychain-test.local"))
                .when().post("/usuarios")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("CP-003 — Rechazar registro de usuario duplicado")
    void cpHu01003_rechazarRegistroDuplicado() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String email = unico("hu01-003") + "@supplychain-test.local";
        registrarUsuario(tokenAdmin, email, "QA Usuario HU01-003", "ClaveSegura2026!");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"email": "%s", "nombreCompleto": "Otro Nombre", "password": "OtraClaveSegura2026!"}
                        """.formatted(email))
                .when().post("/usuarios")
                .then().statusCode(409);
    }

    @Test
    @DisplayName("CP-004 — Rechazar registro de usuario con información inválida")
    void cpHu01004_rechazarRegistroInformacionInvalida() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"email": "%s", "nombreCompleto": "QA Invalido", "password": "corta"}
                        """.formatted(unico("hu01-004") + "@supplychain-test.local"))
                .when().post("/usuarios")
                .then().statusCode(400);
    }
}
