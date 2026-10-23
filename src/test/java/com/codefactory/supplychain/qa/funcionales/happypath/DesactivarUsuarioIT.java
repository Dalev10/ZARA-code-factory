package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-30 y HU-31 (Sprint 2). Un usuario desactivado deja de operar de inmediato aunque su token
 * siga vigente (401 con WWW-Authenticate: Bearer). Nadie se desactiva a sí mismo y nunca se
 * desactiva al último ADMIN activo.
 */
@Tag("funcional")
@Tag("happy-path")
class DesactivarUsuarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-003 — Reactivar un usuario le devuelve el acceso con un inicio de sesión nuevo")
    void cpHu31001_reactivarUsuario() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu31-001", "VENDEDOR");
        given().header("Authorization", bearer(tokenAdmin))
                .when().put("/usuarios/" + vendedor.id() + "/desactivar")
                .then().statusCode(200);

        given().header("Authorization", bearer(tokenAdmin))
                .when().put("/usuarios/" + vendedor.id() + "/activar")
                .then().statusCode(200)
                .body("estado", equalTo("ACTIVO"));

        String tokenNuevo = iniciarSesion(vendedor.email(), PASSWORD);
        given().header("Authorization", bearer(tokenNuevo))
                .when().get("/ventas")
                .then().statusCode(200);
    }
}
