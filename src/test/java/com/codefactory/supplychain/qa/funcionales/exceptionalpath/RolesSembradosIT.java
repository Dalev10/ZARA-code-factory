package com.codefactory.supplychain.qa.funcionales.exceptionalpath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-33 (Sprint 2). Los roles VENDEDOR, SUPERVISOR_TIENDA y OPERADOR_CD vienen sembrados con los
 * scopes de la guía; sus nombres son reservados. El campo {@code sensible} de un scope es opcional.
 */
@Tag("funcional")
@Tag("exceptional-path")
class RolesSembradosIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-006 — Crear un scope sin el campo sensible lo deja en false")
    void cpHu33006_scopeSinSensibleQuedaEnFalse() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"codigo": "%s", "descripcion": "Scope QA sin sensible"}
                        """.formatted(unico("qa:scope")))
                .when().post("/scopes")
                .then().statusCode(201)
                .body("sensible", equalTo(false));
    }
}
