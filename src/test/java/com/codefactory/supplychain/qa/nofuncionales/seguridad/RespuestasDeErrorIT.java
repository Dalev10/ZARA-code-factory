package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-29 (Sprint 2). Contrato transversal de errores de la guía: todo error trae
 * {"mensaje": "..."} en español neutro y llega con su código real (un 404 o 405 con usuario
 * autenticado ya no se convierte en 401). Las fechas se responden en UTC con Z.
 */
@Tag("no-funcional")
@Tag("seguridad")
class RespuestasDeErrorIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-002 — Sin token responde 401 con WWW-Authenticate: Bearer")
    void cpHu29002_sinTokenResponde401Bearer() {
        given().when().get("/ventas")
                .then().statusCode(401)
                .header("WWW-Authenticate", containsString("Bearer"));
    }

    @Test
    @DisplayName("CP-005 — Falta de scope responde 403 con mensaje en español neutro")
    void cpHu29005_faltaDeScopeConMensajeNeutro() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Usuario vendedor = usuarioConRol(tokenAdmin, "hu29-005", "VENDEDOR");

        given().header("Authorization", bearer(vendedor.token()))
                .when().get("/umbrales-reposicion")
                .then().statusCode(403)
                .body("mensaje", equalTo("No tiene permisos para realizar esta acción"));
    }
}
