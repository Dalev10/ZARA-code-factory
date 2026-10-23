package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@Tag("funcional")
@Tag("happy-path")
class RegistrarUsuarioIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — Registrar usuario con información válida, habilitado y con su rol")
    void cpHu01001_registrarUsuarioValidoHabilitadoConRol() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String email = unico("hu01-001") + "@supplychain-test.local";

        String usuarioId = registrarUsuario(tokenAdmin, email, "QA Usuario HU01-001", "ClaveSegura2026!");

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
}
