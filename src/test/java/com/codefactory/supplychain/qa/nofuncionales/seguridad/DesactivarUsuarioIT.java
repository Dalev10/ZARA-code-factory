package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * HU-30 y HU-31 (Sprint 2). Un usuario desactivado deja de operar de inmediato aunque su token
 * siga vigente (401 con WWW-Authenticate: Bearer). Nadie se desactiva a sí mismo y nunca se
 * desactiva al último ADMIN activo.
 */
@Tag("no-funcional")
@Tag("seguridad")
class DesactivarUsuarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Un usuario desactivado pierde el acceso con su token vigente (401 Bearer)")
    void cpHu30001_tokenVigenteDeUsuarioDesactivadoResponde401() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu30-001", "VENDEDOR");

        given().header("Authorization", bearer(vendedor.token()))
                .when().get("/ventas")
                .then().statusCode(200);

        given().header("Authorization", bearer(tokenAdmin))
                .when().put("/usuarios/" + vendedor.id() + "/desactivar")
                .then().statusCode(200)
                .body("estado", equalTo("INACTIVO"));

        given().header("Authorization", bearer(vendedor.token()))
                .when().get("/ventas")
                .then().statusCode(401)
                .header("WWW-Authenticate", containsString("Bearer"));
    }

    @Test
    @DisplayName("CP-002 — Un usuario desactivado no puede iniciar sesión")
    void cpHu30002_usuarioDesactivadoNoIniciaSesion() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu30-002", "VENDEDOR");
        given().header("Authorization", bearer(tokenAdmin))
                .when().put("/usuarios/" + vendedor.id() + "/desactivar")
                .then().statusCode(200);

        given().contentType("application/json")
                .body("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(vendedor.email(), PASSWORD))
                .when().post("/auth/login")
                .then().statusCode(401)
                .body("mensaje", notNullValue());
    }
}
