package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * HU-30 y HU-31 (Sprint 2). Un usuario desactivado deja de operar de inmediato aunque su token
 * siga vigente (401 con WWW-Authenticate: Bearer). Nadie se desactiva a sí mismo y nunca se
 * desactiva al último ADMIN activo.
 */
@Tag("funcional")
@Tag("negative-path")
class DesactivarUsuarioIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-004 — Rechazar que un usuario se desactive a sí mismo")
    void cpHu31002_rechazarAutodesactivacion() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Usuario gestor = usuarioConScopes(tokenAdmin, "hu31-002", "usuarios:administrar");

        given().header("Authorization", bearer(gestor.token()))
                .when().put("/usuarios/" + gestor.id() + "/desactivar")
                .then().statusCode(anyOf(is(400), is(409)))
                .body("mensaje", notNullValue());

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/usuarios/" + gestor.id())
                .then().statusCode(200)
                .body("estado", equalTo("ACTIVO"));
    }

    /**
     * El ADMIN sembrado es el único usuario con rol ADMIN en la suite. Si la regla fallara, el
     * bloque finally lo reactiva para no tumbar el resto de casos que comparten la base.
     */
    @Test
    @DisplayName("CP-005 — Rechazar la desactivación del último ADMIN activo")
    void cpHu31003_rechazarDesactivarUltimoAdmin() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Usuario gestor = usuarioConScopes(tokenAdmin, "hu31-003", "usuarios:administrar");
        String adminId = given().header("Authorization", bearer(tokenAdmin))
                .when().get("/usuarios")
                .then().statusCode(200)
                .extract().path("find { it.email == '" + ADMIN_EMAIL + "' }.id");

        try {
            given().header("Authorization", bearer(gestor.token()))
                    .when().put("/usuarios/" + adminId + "/desactivar")
                    .then().statusCode(anyOf(is(400), is(409)))
                    .body("mensaje", notNullValue());

            given().header("Authorization", bearer(gestor.token()))
                    .when().get("/usuarios/" + adminId)
                    .then().statusCode(200)
                    .body("estado", equalTo("ACTIVO"));
        } finally {
            given().header("Authorization", bearer(gestor.token()))
                    .when().put("/usuarios/" + adminId + "/activar");
        }
    }

    @Test
    @DisplayName("CP-006 — Desactivar un usuario inexistente responde 404")
    void cpHu31004_desactivarUsuarioInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .when().put("/usuarios/" + java.util.UUID.randomUUID() + "/desactivar")
                .then().statusCode(404)
                .body("mensaje", notNullValue());
    }
}
