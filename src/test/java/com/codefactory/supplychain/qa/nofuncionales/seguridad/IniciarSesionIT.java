package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@Tag("no-funcional")
@Tag("seguridad")
class IniciarSesionIT extends AbstractCasoPruebaIT {

    private static String email;
    private static final String PASSWORD = "ClaveSegura2026!";

    private void asegurarFixture(String tokenAdmin) {
        if (email == null) {
            String[] datos = crearUsuarioConUnScope(tokenAdmin, "hu02", "cd:administrar", PASSWORD);
            email = datos[0];
        }
    }

    @Test
    @DisplayName("CP-002 — Rechazar inicio de sesión con contraseña incorrecta")
    void cpHu02002_rechazarPasswordIncorrecta() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().contentType("application/json")
                .body("""
                        {"email": "%s", "password": "ClaveIncorrecta999!"}
                        """.formatted(email))
                .when().post("/auth/login")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("CP-003 — Rechazar inicio de sesión de un usuario no registrado")
    void cpHu02003_rechazarUsuarioNoRegistrado() {
        given().contentType("application/json")
                .body("""
                        {"email": "%s", "password": "ClaveCualquiera2026!"}
                        """.formatted(unico("hu02-003-inexistente") + "@supplychain-test.local"))
                .when().post("/auth/login")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("CP-004 — Impedir el acceso a una función no permitida para el rol del usuario")
    void cpHu02004_impedirAccesoFuncionNoPermitida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);
        String token = iniciarSesion(email, PASSWORD);

        given().header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {"nombre": "Tienda No Autorizada QA", "ubicacion": "No debería crearse"}
                        """)
                .when().post("/tiendas")
                .then().statusCode(403);
    }
}
