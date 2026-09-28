package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

/**
 * HU-01 — Registrar Usuario. Casos CP-HU01-001 a CP-HU01-004 del documento
 * «Casos_de_Prueba_-_Sprint__1_EAP01FE.docx».
 */
class Hu01RegistrarUsuarioIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-HU01-001 — Registrar usuario con información válida, habilitado y con su rol")
    void cpHu01001_registrarUsuarioValidoHabilitadoConRol() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String email = unico("hu01-001") + "@supplychain-test.local";

        // AC-1 / AC-4: se registra y queda habilitado (estado ACTIVO) para iniciar sesión.
        String usuarioId = registrarUsuario(tokenAdmin, email, "QA Usuario HU01-001", "ClaveSegura2026!");

        // El registro por sí solo NO asocia rol (RegistrarUsuarioRequest no tiene ese campo);
        // AC-5 se cumple con una llamada adicional a POST /usuarios/{id}/roles.
        String rolId = crearRol(tokenAdmin, unico("hu01-001-rol"));
        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"rolId": "%s"}
                        """.formatted(rolId))
                .when().post("/usuarios/" + usuarioId + "/roles")
                .then().statusCode(204);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/usuarios/" + usuarioId + "/roles")
                .then().statusCode(200)
                .body("id", hasItem(rolId));

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/usuarios/" + usuarioId)
                .then().statusCode(200)
                .body("estado", equalTo("ACTIVO"))
                .body("email", equalTo(email));
    }

    @Test
    @DisplayName("CP-HU01-002 — Rechazar registro de usuario con información obligatoria incompleta")
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
    @DisplayName("CP-HU01-003 — Rechazar registro de usuario duplicado")
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
    @DisplayName("CP-HU01-004 — Rechazar registro de usuario con información inválida")
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
