package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@Tag("funcional")
@Tag("happy-path")
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
    @DisplayName("CP-001 — Iniciar sesión con credenciales válidas y acceder a funciones permitidas")
    void cpHu02001_iniciarSesionValidaYAccederFuncionPermitida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        String token = iniciarSesion(email, PASSWORD);

        given().header("Authorization", "Bearer " + token)
                .when().get("/centros-distribucion")
                .then().statusCode(200);
    }
}
