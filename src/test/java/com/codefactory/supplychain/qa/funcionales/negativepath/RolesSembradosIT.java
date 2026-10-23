package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * HU-33 (Sprint 2). Los roles VENDEDOR, SUPERVISOR_TIENDA y OPERADOR_CD vienen sembrados con los
 * scopes de la guía; sus nombres son reservados. El campo {@code sensible} de un scope es opcional.
 */
@Tag("funcional")
@Tag("negative-path")
class RolesSembradosIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-005 — Rechazar crear un rol con un nombre reservado (409)")
    void cpHu33005_rechazarRolConNombreReservado() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "VENDEDOR", "descripcion": "Duplicado"}
                        """)
                .when().post("/roles")
                .then().statusCode(409);
    }
}
